package com.xigua.center.chatMessage;

import com.xigua.api.service.ChatMessageService;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.enums.ContentType;
import com.xigua.domain.ws.MessageRequest;
import com.xigua.domain.entity.ChatMessage;
import com.xigua.domain.enums.ChatType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @ClassName AbstractChatMessageService
 * @Description 聊天消息 抽象类
 * @Author wangjinfei
 * @Date 2025/8/17 15:45
 */
public abstract class AbstractSendChatMessageService {
    @Autowired
    private Sequence sequence;
    @Autowired
    private ChatMessageService chatMessageService;

    /**
     * 获取聊天类型
     * @author wangjinfei
     * @date 2025/8/17 16:16
     * @return ChatType
    */
    protected abstract ChatType getChatType();

    /**
     * 保存未读消息
     * @author wangjinfei
     * @date 2025/8/17 15:49
     * @param messageRequest
     * @return Boolean
    */
    public abstract Boolean saveMessageUnread(MessageRequest messageRequest);

    /**
     * 最后一条消息  (消息列表)
     * @author wangjinfei
     * @date 2025/8/17 17:09
     * @param messageRequest
    */
    public abstract void lastMessage(MessageRequest messageRequest);

    /**
     * 聊天消息发送到接收人
     * @author wangjinfei
     * @date 2025/8/17 17:14
     * @param messageRequest
    */
    public abstract void chatMessage2Receiver(MessageRequest messageRequest);

    /**
     * 未读消息数量
     * @author wangjinfei
     * @date 2025/8/17 17:25
     * @param messageRequest
    */
    public abstract void unreadMessageCount(MessageRequest messageRequest);

    /**
     * 消息未读到消息已读
     * @author wangjinfei
     * @date 2025/8/17 16:33
     * @param messageRequest
     */
    public abstract void unread2Read(MessageRequest messageRequest);

    /**
     * 保存聊天消息
     * @author wangjinfei
     * @date 2025/8/17 15:47
     * @param messageRequest
     * @return Boolean
    */
    public Boolean saveChatMessage(MessageRequest messageRequest){
        // 封装聊天消息
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(messageRequest.getChatMessageId());
        chatMessage.setClientMsgId(messageRequest.getClientMsgId());
        chatMessage.setSenderId(messageRequest.getSenderId());
        chatMessage.setReceiverId(messageRequest.getReceiverId());
        chatMessage.setMessage(messageRequest.getMessage());
        chatMessage.setChatType(messageRequest.getChatType());
        chatMessage.setMessageType(messageRequest.getContentType());
        chatMessage.setIp(messageRequest.getIp());
        chatMessage.setReplyToId(messageRequest.getReplyToId());
        chatMessage.setReplyContent(messageRequest.getReplyContent());
        chatMessage.setAtUsers(messageRequest.getAtUsers());
        chatMessage.setWithdrawn(0);
        // ws拿不到threadLocal存储的当前用户 （此时的发送人就是创建人）
        chatMessage.setCreateBy(messageRequest.getSenderId());
        // 添加聊天消息
        boolean save = chatMessageService.save(chatMessage);

        return save;
    }

    /**
     * 最后消息摘要
     * @author wangjinfei
     * @date 2026/2/7 17:57
     * @param request
     * @return String
    */
    public String lastMessageSummary(MessageRequest request) {
        Integer contentType = request.getContentType();
        if (contentType == null) {
            return "[未知消息]";
        }

        if (contentType == ContentType.TEXT.getValue()) {
            return request.getMessage();
        } else if (contentType == ContentType.IMAGE.getValue()) {
            return "[图片]";
        } else if (contentType == ContentType.AUDIO.getValue()) {
            return "[语音]";
        } else if (contentType == ContentType.VIDEO.getValue()) {
            return "[视频]";
        } else if (contentType == ContentType.FILE.getValue()) {
            return "[文件]";
        } else if (contentType == ContentType.CALL.getValue()) {
            // 通话记录消息体是 JSON，会话列表只暴露可读状态，不展示内部字段。
            String body = request.getMessage();
            if (body == null) {
                return "[视频通话]";
            }
            if (body.contains("\"completed\"")) {
                return "[视频通话] 通话结束";
            }
            if (body.contains("\"busy\"")) {
                return "[视频通话] 忙线未接";
            }
            if (body.contains("\"rejected\"") || body.contains("\"timeout\"") || body.contains("\"missed\"")) {
                return "[视频通话] 未接听";
            }
            if (body.contains("\"canceled\"")) {
                return "[视频通话] 已取消";
            }
            return "[视频通话] 未接通";
        } else if (contentType == ContentType.CHAT_RECORD.getValue()) {
            return "[聊天记录]";
        } else if (contentType == ContentType.VOTE.getValue()) {
            String message = request.getMessage();
            if (message == null || message.trim().isEmpty()) {
                return "[群投票]";
            }
            String title = message.replace("【投票】", "").split("\\|\\|")[0].trim();
            return title.isEmpty() ? "[群投票]" : "[群投票] " + title;
        } else {
            return "[未知消息]";
        }
    }

}
