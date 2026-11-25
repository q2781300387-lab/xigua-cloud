package com.xigua.center.audio;

import com.xigua.api.service.CenterService;
import com.xigua.center.wsMessage.AbstractMessageService;
import com.xigua.common.core.util.DateUtil;
import com.xigua.domain.connect.Client;
import com.xigua.domain.enums.MessageSubType;
import com.xigua.domain.enums.MessageType;
import com.xigua.domain.ws.MessageRequest;
import com.xigua.domain.ws.MessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 音频呼叫超时未接听：被叫方 10 秒无应答后发送，通知主叫方「对方超时未接听」
 */
@Component
public class AudioCallTimeoutMessageService extends AbstractMessageService {
    @Autowired
    private CenterService centerService;

    @Override
    public String getMessageName() {
        return MessageSubType.AUDIO_CALL_TIMEOUT.getDesc();
    }

    @Override
    public String getMessageType() {
        return MessageType.AUDIO_CALL.getType();
    }

    @Override
    public String getMessageSubType() {
        return MessageSubType.AUDIO_CALL_TIMEOUT.getType();
    }

    @Override
    public void handleMessage(MessageRequest messageRequest) {
        String receiverId = messageRequest.getReceiverId();
        Client client = centerService.onlineUser(receiverId);
        if (client == null) {
            return;
        }

        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setCallId(messageRequest.getCallId());
        messageResponse.setSenderId(messageRequest.getSenderId());
        messageResponse.setReceiverId(receiverId);
        messageResponse.setMessage("");
        messageResponse.setMessageType(MessageType.AUDIO_CALL.getType());
        messageResponse.setSubType(MessageSubType.AUDIO_CALL_TIMEOUT.getType());
        messageResponse.setCreateTime(DateUtil.formatDateTime(LocalDateTime.now(), DateUtil.DATE_TIME_FORMATTER));
        centerService.sendMessage2Client(messageResponse, client);
    }
}
