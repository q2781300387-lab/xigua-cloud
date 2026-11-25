package com.xigua.center.video;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.CenterService;
import com.xigua.center.wsMessage.AbstractMessageService;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.connect.Client;
import com.xigua.domain.enums.MessageSubType;
import com.xigua.domain.enums.MessageType;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.ws.MessageRequest;
import com.xigua.domain.ws.MessageResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @ClassName VideoCallCanceledMessageService
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/17 18:17
 */
@Component
public class VideoCallCanceledMessageService extends AbstractMessageService {
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private CenterService centerService;

    @Override
    public String getMessageName() {
        return MessageSubType.VIDEO_CALL_CANCELED.getDesc();
    }

    @Override
    public String getMessageType() {
        return MessageType.VIDEO_CALL.getType();
    }

    @Override
    public String getMessageSubType() {
        return MessageSubType.VIDEO_CALL_CANCELED.getType();
    }

    /**
     * 视频呼叫已取消
     * @author wangjinfei
     * @date 2025/6/2 20:38
     * @param messageRequest
     */
    @Override
    public void handleMessage(MessageRequest messageRequest) {
        String senderId = messageRequest.getSenderId();
        String receiverId = messageRequest.getReceiverId();

        // 发送取消
        canceled(messageRequest);
    }

    private void canceled(MessageRequest messageRequest){
        String senderId = messageRequest.getSenderId();
        String receiverId = messageRequest.getReceiverId();

        // 发送方
        Client senderClient = centerService.onlineUser(senderId);
        if(senderClient != null) {
            MessageResponse messageResponse = new MessageResponse();
            messageResponse.setCallId(messageRequest.getCallId());
            messageResponse.setSenderId(messageRequest.getSenderId());
            messageResponse.setReceiverId(messageRequest.getReceiverId());
            messageResponse.setMessage("");
            messageResponse.setMessageType(MessageType.VIDEO_CALL.getType());
            messageResponse.setSubType(MessageSubType.VIDEO_CALL_CANCELED.getType());
            messageResponse.setCreateTime(DateUtil.formatDateTime(LocalDateTime.now(), DateUtil.DATE_TIME_FORMATTER));
            centerService.sendMessage2Client(messageResponse, senderClient);
        }

        // 接收方
        Client receiverClient = centerService.onlineUser(receiverId);
        if(receiverClient != null) {
            MessageResponse messageResponse = new MessageResponse();
            messageResponse.setCallId(messageRequest.getCallId());
            messageResponse.setSenderId(messageRequest.getSenderId());
            messageResponse.setReceiverId(messageRequest.getReceiverId());
            messageResponse.setMessage("");
            messageResponse.setMessageType(MessageType.VIDEO_CALL.getType());
            messageResponse.setSubType(MessageSubType.VIDEO_CALL_CANCELED.getType());
            messageResponse.setCreateTime(DateUtil.formatDateTime(LocalDateTime.now(), DateUtil.DATE_TIME_FORMATTER));
            centerService.sendMessage2Client(messageResponse, receiverClient);
        }
    }
}