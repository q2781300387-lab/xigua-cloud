package com.xigua.center.audio;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.CenterService;
import com.xigua.center.wsMessage.AbstractMessageService;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.bo.SenderBO;
import com.xigua.domain.connect.Client;
import com.xigua.domain.enums.MessageSubType;
import com.xigua.domain.enums.MessageType;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.enums.Sender;
import com.xigua.domain.ws.MessageRequest;
import com.xigua.domain.ws.MessageResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @ClassName AudioCallRequestMessageService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/1/14 19:41
 */
@Component
public class AudioCallRequestMessageService extends AbstractMessageService {
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private CenterService centerService;


    @Override
    public String getMessageName() {
        return MessageSubType.AUDIO_CALL_REQUEST.getDesc();
    }

    @Override
    public String getMessageType() {
        return MessageType.AUDIO_CALL.getType();
    }

    @Override
    public String getMessageSubType() {
        return MessageSubType.AUDIO_CALL_REQUEST.getType();
    }

    /**
     * 音频呼叫请求
     * @author wangjinfei
     * @date 2025/6/2 20:38
     * @param messageRequest
     */
    @Override
    public void handleMessage(MessageRequest messageRequest) {
        String senderId = messageRequest.getSenderId();
        String receiverId = messageRequest.getReceiverId();

        // 对方是否在线
        Client client = centerService.onlineUser(receiverId);
        if(client == null){
            // 未在线 直接拒绝
            rejected(messageRequest);
            return;
        }

        // 发送呼叫
        call(messageRequest);
    }

    private void rejected(MessageRequest messageRequest){
        String senderId = messageRequest.getSenderId();

        Client client = centerService.onlineUser(senderId);

        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setCallId(messageRequest.getCallId());
        messageResponse.setSenderId(Sender.SYSTEM.getSender());
        messageResponse.setReceiverId(senderId);
        messageResponse.setMessage("");
        messageResponse.setMessageType(MessageType.AUDIO_CALL.getType());
        messageResponse.setSubType(MessageSubType.AUDIO_CALL_REJECTED.getType());
        messageResponse.setCreateTime(DateUtil.formatDateTime(LocalDateTime.now(), DateUtil.DATE_TIME_FORMATTER));
        centerService.sendMessage2Client(messageResponse, client);
    }

    private void call(MessageRequest messageRequest){
        String senderId = messageRequest.getSenderId();
        String receiverId = messageRequest.getReceiverId();

        Client client = centerService.onlineUser(receiverId);

        String userCache = redisClient.get(RedisEnum.USER.getKey() + senderId);
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setCallId(messageRequest.getCallId());
        messageResponse.setSenderId(senderId);
        if(StringUtils.isNotEmpty(userCache)){
            SenderBO sender = JSONObject.parseObject(userCache, SenderBO.class);
            messageResponse.setSender(sender);
        }
        messageResponse.setReceiverId(receiverId);
        messageResponse.setMessage("");
        messageResponse.setMessageType(MessageType.AUDIO_CALL.getType());
        messageResponse.setSubType(MessageSubType.AUDIO_CALL_INCOMING.getType());
        messageResponse.setCreateTime(DateUtil.formatDateTime(LocalDateTime.now(), DateUtil.DATE_TIME_FORMATTER));
        centerService.sendMessage2Client(messageResponse, client);
    }
}
