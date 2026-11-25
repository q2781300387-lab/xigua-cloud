package com.xigua.center.video;

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
 * 视频呼叫超时未接听。
 *
 * 呼叫方和被叫方都可能先到达本地等待上限：被叫方超时后主动广播 timeout，
 * 呼叫方无需再等满自己的 30 秒计时器，也不会把“超时”误显示成“对方拒绝”。
 */
@Component
public class VideoCallTimeoutMessageService extends AbstractMessageService {

    @Autowired
    private CenterService centerService;

    @Override
    public String getMessageName() {
        return MessageSubType.VIDEO_CALL_TIMEOUT.getDesc();
    }

    @Override
    public String getMessageType() {
        return MessageType.VIDEO_CALL.getType();
    }

    @Override
    public String getMessageSubType() {
        return MessageSubType.VIDEO_CALL_TIMEOUT.getType();
    }

    @Override
    public void handleMessage(MessageRequest messageRequest) {
        // 发起超时的一方已经在本地收尾，只需要通知通话另一端。
        forwardTo(messageRequest, messageRequest.getReceiverId());
    }

    private void forwardTo(MessageRequest request, String userId) {
        if (userId == null || userId.isEmpty()) return;
        Client client = centerService.onlineUser(userId);
        if (client == null) return;

        MessageResponse response = new MessageResponse();
        response.setCallId(request.getCallId());
        response.setSenderId(request.getSenderId());
        response.setReceiverId(request.getReceiverId());
        response.setMessage("");
        response.setMessageType(MessageType.VIDEO_CALL.getType());
        response.setSubType(MessageSubType.VIDEO_CALL_TIMEOUT.getType());
        response.setCreateTime(DateUtil.formatDateTime(LocalDateTime.now(), DateUtil.DATE_TIME_FORMATTER));
        centerService.sendMessage2Client(response, client);
    }
}
