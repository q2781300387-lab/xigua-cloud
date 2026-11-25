package com.xigua.center.wsMessage;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.CenterService;
import com.xigua.api.service.GroupMemberService;
import com.xigua.api.service.UserService;
import com.xigua.domain.connect.Client;
import com.xigua.domain.entity.User;
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

import java.util.Set;

/**
 * @ClassName TypingChatMessageService
 * @Description 「对方正在输入」事件转发（typing）：不落库，只把输入状态转发给会话对端。
 *              单聊转给对方；群聊转给除发送者外的群成员。接收端以 5 秒超时自行消失，
 *              因此不做「停止输入」帧，避免多端并发下互相覆盖。
 * @Author doubao
 * @Date 2026/11/12
 */
@Slf4j
@Component
public class TypingChatMessageService extends AbstractMessageService {

    @Autowired
    private CenterService centerService;
    @DubboReference
    private GroupMemberService groupMemberService;
    @DubboReference
    private UserService userService;

    @Override
    public String getMessageName() {
        return MessageSubType.TYPING.getDesc();
    }

    @Override
    public String getMessageType() {
        return MessageType.CHAT.getType();
    }

    @Override
    public String getMessageSubType() {
        return MessageSubType.TYPING.getType();
    }

    @Override
    public void handleMessage(MessageRequest messageRequest) {
        String senderId = messageRequest.getSenderId();
        String receiverId = messageRequest.getReceiverId();
        Integer chatType = messageRequest.getChatType();
        if (StringUtils.isEmpty(senderId) || StringUtils.isEmpty(receiverId) || chatType == null) {
            return;
        }

        // 事件体只带定位会话所需的信息：发送者、会话 id、会话类型
        JSONObject payload = new JSONObject();
        payload.put("senderId", senderId);
        payload.put("receiverId", receiverId);
        payload.put("chatType", chatType);
        // 带上昵称，接收端群聊可直接显示「张三正在输入…」，不必再回查成员列表
        payload.put("senderName", resolveSenderName(senderId));
        String messageJson = payload.toJSONString();

        if (chatType == ChatType.GROUP.getType()) {
            // 群聊：除自己以外的成员都要看到「某某正在输入」
            Set<String> memberIds = groupMemberService.getGroupMembers(receiverId);
            if (CollectionUtils.isEmpty(memberIds)) {
                return;
            }
            for (String memberId : memberIds) {
                if (StringUtils.equals(memberId, senderId)) {
                    continue;
                }
                pushTyping(memberId, messageJson);
            }
        } else {
            // 单聊：只推给对端，自己不需要看到自己的输入态
            if (StringUtils.equals(senderId, receiverId)) {
                return;
            }
            pushTyping(receiverId, messageJson);
        }
    }

    /**
     * 取发送人昵称。查不到时返回空串，接收端会兜底成「有人」。
     * @param senderId 发送人 id
     */
    private String resolveSenderName(String senderId) {
        try {
            User user = userService.getById(senderId);
            return user == null || StringUtils.isEmpty(user.getUsername()) ? "" : user.getUsername();
        } catch (Exception e) {
            log.warn("--------->>>>>>> 查询输入者昵称失败，senderId={}", senderId, e);
            return "";
        }
    }

    /**
     * 向在线用户推送输入状态。typing 是瞬时状态，对端离线时直接丢弃，
     * 不落库也不补发，避免上线后收到一堆过期「正在输入」。
     * @param targetId 目标用户 id
     * @param messageJson 事件内容
     */
    private void pushTyping(String targetId, String messageJson) {
        Client client = centerService.onlineUser(targetId);
        if (client == null) {
            return;
        }
        MessageResponse response = new MessageResponse();
        response.setSenderId(Sender.SYSTEM.getSender());
        response.setReceiverId(targetId);
        response.setMessageType(MessageType.CHAT.getType());
        response.setSubType(MessageSubType.TYPING.getType());
        response.setMessage(messageJson);
        response.setCreateTime(String.valueOf(System.currentTimeMillis()));
        try {
            centerService.sendMessage2Client(response, client);
        } catch (Exception e) {
            log.error("--------->>>>>>> 推送输入状态失败，targetId={}", targetId, e);
        }
    }
}
