package com.xigua.center.ai;

import com.xigua.api.service.ChatMessageService;
import com.xigua.center.wsMessage.AbstractMessageService;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.entity.ChatMessage;
import com.xigua.domain.enums.ChatType;
import com.xigua.domain.enums.MessageSubType;
import com.xigua.domain.enums.MessageType;
import com.xigua.domain.ws.MessageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @ClassName StopBotResponseMessageService
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/25 18:47
 */
@Component
public class BotResponseEndMessageService extends AbstractMessageService {
    @Autowired
    private Sequence sequence;
    @Autowired
    private ChatMessageService chatMessageService;

    @Override
    public String getMessageName() {
        return MessageSubType.BOT_RESPONSE_END.getDesc();
    }

    @Override
    public String getMessageType() {
        return MessageType.CHAT.getType();
    }

    @Override
    public String getMessageSubType() {
        return MessageSubType.BOT_RESPONSE_END.getType();
    }

    @Override
    public void handleMessage(MessageRequest messageRequest) {
        saveMes(messageRequest);
    }

    private void saveMes(MessageRequest messageRequest){
        // 封装聊天消息
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(sequence.nextNo());
        chatMessage.setSenderId(messageRequest.getSenderId());
        chatMessage.setReceiverId(messageRequest.getReceiverId());
        chatMessage.setMessage(messageRequest.getMessage());
        chatMessage.setChatType(ChatType.BOT.getType());
        chatMessage.setIp(messageRequest.getIp());
        // ws拿不到threadLocal存储的当前用户 （此时的发送人就是创建人）
        chatMessage.setCreateBy(messageRequest.getSenderId());
        // 添加聊天消息
        boolean save = chatMessageService.save(chatMessage);
    }
}
