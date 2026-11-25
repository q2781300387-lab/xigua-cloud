package com.xigua.center.wsMessage;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.CenterService;
import com.xigua.api.service.ChatMessageService;
import com.xigua.api.service.GroupMemberService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.domain.connect.Client;
import com.xigua.domain.entity.ChatMessage;
import com.xigua.domain.enums.ChatType;
import com.xigua.domain.enums.MessageSubType;
import com.xigua.domain.enums.MessageType;
import com.xigua.domain.enums.Sender;
import com.xigua.domain.ws.MessageRequest;
import com.xigua.domain.ws.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * @ClassName WithdrawChatMessageService
 * @Description 消息撤回服务（mes_withdraw）：仅发送者可撤回，撤回后推送撤回事件给会话双方/群成员
 * @Author doubao
 * @Date 2026/11/4
 */
@Slf4j
@Component
public class WithdrawChatMessageService extends AbstractMessageService {

    /** 撤回时限（分钟）：与客户端一致的微信式规则；服务端必须强校验，避免绕过界面撤回历史消息 */
    private static final long WITHDRAW_WINDOW_MINUTES = 2L;

    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private CenterService centerService;
    @DubboReference
    private GroupMemberService groupMemberService;

    @Override
    public String getMessageName() {
        return MessageSubType.MES_WITHDRAW.getDesc();
    }

    @Override
    public String getMessageType() {
        return MessageType.CHAT.getType();
    }

    @Override
    public String getMessageSubType() {
        return MessageSubType.MES_WITHDRAW.getType();
    }

    @Override
    public void handleMessage(MessageRequest messageRequest) {
        String withdrawMessageId = messageRequest.getMessage();
        if (StringUtils.isEmpty(withdrawMessageId)) {
            return;
        }
        String senderId = messageRequest.getSenderId();

        // 查询消息
        ChatMessage chatMessage = chatMessageService.getById(withdrawMessageId);
        if (chatMessage == null) {
            log.warn("--------->>>>>>> 撤回失败：消息不存在，chatMessageId={}", withdrawMessageId);
            return;
        }
        if (chatMessage.getWithdrawn() != null && chatMessage.getWithdrawn() == 1) {
            log.info("--------->>>>>>> 撤回失败：消息已撤回，chatMessageId={}", withdrawMessageId);
            return;
        }

        // 权限校验：仅发送者可撤回
        if (!senderId.equals(chatMessage.getSenderId())) {
            // 非发送者尝试撤回，推送系统消息提示
            pushSystemTip(senderId, "只能撤回自己发送的消息");
            return;
        }

        // 时限校验：仅允许撤回 2 分钟内的消息。前端判断只负责隐藏入口，服务端才是权威约束，
        // 否则绕过界面直接发 mes_withdraw 帧即可撤回任意历史消息。
        if (!isWithinWithdrawWindow(chatMessage)) {
            log.info("--------->>>>>>> 撤回失败：超过时限，chatMessageId={}, createTime={}",
                    withdrawMessageId, chatMessage.getCreateTime());
            pushSystemTip(senderId, "消息已超过 2 分钟，无法撤回");
            return;
        }

        // 更新为已撤回
        chatMessageService.lambdaUpdate()
                .eq(ChatMessage::getId, withdrawMessageId)
                .set(ChatMessage::getWithdrawn, 1)
                .update();

        // 推送撤回事件给会话双方/群成员
        notifyWithdraw(chatMessage);
    }

    /**
     * 判断消息是否仍在可撤回时限内。时间缺失时按超时处理（宁拒绝不放开），
     * 避免脏数据成为绕过时限的后门。
     * @param chatMessage 待撤回消息
     * @return 是否在时限内
     */
    private boolean isWithinWithdrawWindow(ChatMessage chatMessage) {
        LocalDateTime createTime = chatMessage.getCreateTime();
        if (createTime == null) {
            return false;
        }
        return !createTime.plusMinutes(WITHDRAW_WINDOW_MINUTES).isBefore(LocalDateTime.now());
    }

    /**
     * 向在线用户推送一条系统提示（撤回失败原因等）
     * @param userId 目标用户
     * @param text 提示文案
     */
    private void pushSystemTip(String userId, String text) {
        Client client = centerService.onlineUser(userId);
        if (client == null) {
            return;
        }
        MessageResponse tip = new MessageResponse();
        tip.setSenderId(Sender.SYSTEM.getSender());
        tip.setReceiverId(userId);
        tip.setMessageType(MessageType.CHAT.getType());
        tip.setSubType(MessageSubType.SYSTEM_MES.getType());
        tip.setMessage(text);
        tip.setCreateTime(String.valueOf(System.currentTimeMillis()));
        try {
            centerService.sendMessage2Client(tip, client);
        } catch (Exception e) {
            log.error("--------->>>>>>> 推送系统提示失败，receiverId={}, text={}", userId, text, e);
        }
    }

    /**
     * 推送撤回事件：单聊推给双方，群聊推给群成员
     * @param chatMessage 已撤回的消息
     */
    private void notifyWithdraw(ChatMessage chatMessage) {
        Integer chatType = chatMessage.getChatType();
        String withdrawMessageId = chatMessage.getId();

        // 撤回事件内容：携带被撤回消息id与会话信息，客户端据此定位消息并置为撤回态
        JSONObject payload = new JSONObject();
        payload.put("chatMessageId", withdrawMessageId);
        payload.put("senderId", chatMessage.getSenderId());
        payload.put("receiverId", chatMessage.getReceiverId());
        payload.put("chatType", chatType);
        String messageJson = payload.toJSONString();

        if (chatType != null && chatType == ChatType.GROUP.getType()) {
            // 群聊：推送给所有群成员（含发送者，方便自己一端同步状态）
            Set<String> memberIds = groupMemberService.getGroupMembers(chatMessage.getReceiverId());
            if (CollectionUtils.isEmpty(memberIds)) {
                return;
            }
            for (String memberId : memberIds) {
                pushWithdraw(memberId, messageJson);
            }
        } else {
            // 单聊/机器人：推给发送者与接收者
            pushWithdraw(chatMessage.getSenderId(), messageJson);
            pushWithdraw(chatMessage.getReceiverId(), messageJson);
        }
    }

    /**
     * 向在线用户推送撤回事件
     * @param receiverId 接收人id
     * @param messageJson 撤回事件内容
     */
    private void pushWithdraw(String receiverId, String messageJson) {
        Client client = centerService.onlineUser(receiverId);
        if (client == null) {
            // 接收人不在线，撤回状态已落库，上线后拉取历史消息即可见
            return;
        }
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setSenderId(Sender.SYSTEM.getSender());
        messageResponse.setReceiverId(receiverId);
        messageResponse.setMessageType(MessageType.CHAT.getType());
        messageResponse.setSubType(MessageSubType.MES_WITHDRAW.getType());
        messageResponse.setMessage(messageJson);
        messageResponse.setCreateTime(String.valueOf(System.currentTimeMillis()));
        try {
            centerService.sendMessage2Client(messageResponse, client);
        } catch (Exception e) {
            log.error("--------->>>>>>> 推送撤回事件失败，receiverId={}, chatMessageId={}", receiverId, messageJson, e);
        }
    }
}
