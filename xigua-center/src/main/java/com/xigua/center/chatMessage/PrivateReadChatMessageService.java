package com.xigua.center.chatMessage;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.CenterService;
import com.xigua.api.service.MessageReadService;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.connect.Client;
import com.xigua.domain.enums.*;
import com.xigua.domain.ws.MessageRequest;
import com.xigua.domain.ws.MessageResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @ClassName PrivateReadChatMessageService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/8/24 11:41
 */
@Component
public class PrivateReadChatMessageService extends AbstractReadChatMessageService{
    @Autowired
    private MessageReadService messageReadService;
    @Autowired
    private CenterService centerService;
    @Autowired
    private RedisClient redisClient;

    @Override
    protected ChatType getChatType() {
        return ChatType.PRIVATE;
    }

    /**
     * 通知发送人消息已读
     * @author wangjinfei
     * @date 2025/8/24 11:32
     * @param messageRequest
     */
    @Override
    public void notifySenderOfMessageRead(MessageRequest messageRequest) {
        String senderId = messageRequest.getSenderId();
        String receiverId = messageRequest.getReceiverId();

        Client client = centerService.onlineUser(receiverId);
        if(client == null){
            return;
        }

        // 获取接收人打开的聊天窗口好友是谁
        String receiverActiveFriend = redisClient.get(RedisEnum.CURRENT_CHAT_WINDOW.getKey() + receiverId);
        if(StringUtils.isEmpty(receiverActiveFriend)){
            return;
        }

        // 接收人打开的聊天窗口好友是发送人，发送已读通知  （对方在线 && 对方打开的聊天框是发送人）
        if(receiverActiveFriend.equals(senderId)){

            // 系统推送已读通知
            MessageResponse messageResponse = new MessageResponse();
            messageResponse.setSenderId(Sender.SYSTEM.getSender());
            messageResponse.setReceiverId(receiverId);
            messageResponse.setMessageType(MessageType.CHAT.getType());
            messageResponse.setSubType(MessageSubType.MES_READ.getType());
            String json = JSONObject.of("readChatMessageIds", messageRequest.getMessage().toString()).toJSONString();
            messageResponse.setMessage(json);
            messageResponse.setCreateTime(DateUtil.formatDateTime(LocalDateTime.now(), DateUtil.DATE_TIME_FORMATTER));
            centerService.sendMessage2Client(messageResponse, client);
        }
    }

    /**
     * 清空未读消息数量
     * @author wangjinfei
     * @date 2025/8/24 11:35
     * @param messageRequest
     */
    @Override
    public void clearUnreadCount(MessageRequest messageRequest) {
        String senderId = messageRequest.getSenderId();
        String receiverId = messageRequest.getReceiverId();
        String friendUnreadCountKey = RedisEnum.FRIEND_UNREAD_COUNT.getKey() + senderId;

        // 获取当前未读数（扣减前）
        Object unreadObj = redisClient.hashGet(friendUnreadCountKey, receiverId);
        Long currentUnread = unreadObj instanceof Number ? ((Number) unreadObj).longValue() : null;

        // 清零好友未读数
        redisClient.hashPut(friendUnreadCountKey, receiverId, 0);

        // 扣减总未读数
        if (currentUnread != null && currentUnread > 0) {
            String chatUnreadCountKey = RedisEnum.CHAT_UNREAD_COUNT.getKey() + senderId;
            redisClient.incrby(chatUnreadCountKey, -currentUnread);
        }
    }
}
