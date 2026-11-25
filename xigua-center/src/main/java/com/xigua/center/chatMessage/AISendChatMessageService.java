package com.xigua.center.chatMessage;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.*;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.bo.LastMessageBO;
import com.xigua.domain.bo.LastMessageContentBO;
import com.xigua.domain.connect.Client;
import com.xigua.domain.entity.Bot;
import com.xigua.domain.enums.*;
import com.xigua.domain.ws.MessageRequest;
import com.xigua.domain.ws.MessageResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * @ClassName AISendChatMessage
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/10/1 10:48
 */
@Service
public class AISendChatMessageService extends AbstractSendChatMessageService{
    @Autowired
    private Sequence sequence;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private CenterService centerService;
    @Autowired
    private MessageReadService messageReadService;
    @DubboReference
    private AIService aiService;
    @DubboReference
    private BotService botService;

    @Override
    protected ChatType getChatType() {
        return ChatType.BOT;
    }

    @Override
    public Boolean saveMessageUnread(MessageRequest messageRequest) {
        return null;
    }

    @Override
    public void lastMessage(MessageRequest messageRequest) {
        String senderId = messageRequest.getSenderId();
        String receiverId = messageRequest.getReceiverId();
        long timestamp = System.currentTimeMillis();

        // 存储redis 最后消息 （接收人）
        redisClient.zsadd(RedisEnum.LAST_MES.getKey() + receiverId, senderId, timestamp);
        // 最后消息内容
        LastMessageBO lastMessage4Sender = buildLastMessage4Receiver(messageRequest);
        redisClient.hashPut(RedisEnum.LAST_MES_CONTENT.getKey() + receiverId, senderId, JSONObject.toJSONString(lastMessage4Sender));

        // 存储redis 最后消息 （发送人）
        redisClient.zsadd(RedisEnum.LAST_MES.getKey() + senderId, receiverId, timestamp);
        LastMessageBO lastMessage4Receiver = buildLastMessage4Sender(messageRequest);
        redisClient.hashPut(RedisEnum.LAST_MES_CONTENT.getKey() + senderId, receiverId, JSONObject.toJSONString(lastMessage4Receiver));

    }

    @Override
    public void chatMessage2Receiver(MessageRequest messageRequest) {
        // 获取接收人所在的节点信息
        Client client = centerService.onlineUser(messageRequest.getSenderId());
        if(client == null){
            // 如果接收人不在线，直接返回，不做后续处理
            return;
        }

        // 获取机器人缓存信息
        String botCache = redisClient.get(RedisEnum.BOT.getKey() + messageRequest.getReceiverId());
        Bot bot;
        if(StringUtils.isEmpty(botCache)){
            // 缓存丢失时 bot 会是 null，下面 bot.getPrompt() 直接抛空指针，整条 AI 回复都发不出去
            bot = botService.getById(messageRequest.getReceiverId());
            if(bot == null){
                // 库里也没有这个机器人，无法构造提示词，直接结束
                return;
            }
            redisClient.set(RedisEnum.BOT.getKey() + messageRequest.getReceiverId(), JSONObject.toJSONString(bot));
        }else{
            bot = JSONObject.parseObject(botCache, Bot.class);
        }

        Mono<AgentRequest> agentReq = Mono.just(
                AgentRequest.newBuilder()
                        .setRequestId(sequence.nextNo())
                        .setInput(messageRequest.getMessage())
                        .setStream(true)
                        .setPrompt(bot.getPrompt())
                        .build()
        );
        Flux<AgentResponse> chatFlux = aiService.agentProcess(agentReq);
        // todo 暂时用一个新id 来表示一次ai的会话
        String sessionId = sequence.nextNo();
        chatFlux.subscribe(output -> {
            // 实时推送消息，发送到接收人所在节点
            MessageResponse messageResponse = new MessageResponse();
            messageResponse.setSenderId(messageRequest.getReceiverId());
            messageResponse.setReceiverId(messageRequest.getSenderId());
            messageResponse.setMessageType(MessageType.CHAT.getType());
            messageResponse.setChatType(ChatType.BOT.getType());
            messageResponse.setSubType(MessageSubType.MES_RECEIVE.getType());
            messageResponse.setMessage(output.getOutput());
            messageResponse.setChatMessageId(sessionId);
            messageResponse.setCreateTime(DateUtil.formatDateTime(LocalDateTime.now(), DateUtil.DATE_TIME_FORMATTER));
            centerService.sendMessage2Client(messageResponse, client);
        });
    }

    @Override
    public void unreadMessageCount(MessageRequest messageRequest) {

    }

    @Override
    public void unread2Read(MessageRequest messageRequest) {

    }

    private LastMessageBO buildLastMessage4Receiver(MessageRequest messageRequest){
        LastMessageBO lastMessageBO = new LastMessageBO();
        LastMessageContentBO lastMessageContent = new LastMessageContentBO();
        String senderId = messageRequest.getSenderId();

        lastMessageBO.setChatId(senderId);
        lastMessageBO.setChatType(ChatType.BOT.getType());
        lastMessageBO.setUpdateTime(System.currentTimeMillis());

        // 用户信息
        String botCache = redisClient.get(RedisEnum.BOT.getKey() + senderId);
        Bot bot = StringUtils.isNotEmpty(botCache) ? JSONObject.parseObject(botCache, Bot.class) : null;
        if (bot == null) {
            // 缓存丢失时这里会把会话名置空，列表里机器人变成无名条目，回查一次数据库并回填
            bot = botService.getById(senderId);
            if (bot != null) {
                redisClient.set(RedisEnum.BOT.getKey() + senderId, JSONObject.toJSONString(bot));
            }
        }
        if (bot != null) {
            lastMessageBO.setChatName(bot.getName());
            lastMessageBO.setAvatar(bot.getAvatar());
        } else {
            lastMessageBO.setAvatar("");
            lastMessageBO.setChatName("");
        }

        // 封装 最后消息内容
        lastMessageContent.setContent(messageRequest.getMessage());
        lastMessageBO.setLastMessageContent(lastMessageContent);

        return lastMessageBO;
    }

    private LastMessageBO buildLastMessage4Sender(MessageRequest messageRequest){
        LastMessageBO lastMessageBO = new LastMessageBO();
        LastMessageContentBO lastMessageContent = new LastMessageContentBO();
        String receiverId = messageRequest.getReceiverId();

        lastMessageBO.setChatId(receiverId);
        lastMessageBO.setChatType(ChatType.BOT.getType());
        lastMessageBO.setUpdateTime(System.currentTimeMillis());

        // 用户信息
        String botCache = redisClient.get(RedisEnum.BOT.getKey() + receiverId);
        Bot bot = StringUtils.isNotEmpty(botCache) ? JSONObject.parseObject(botCache, Bot.class) : null;
        if (bot == null) {
            // 缓存丢失时这里会把会话名置空，回查一次数据库并回填
            bot = botService.getById(receiverId);
            if (bot != null) {
                redisClient.set(RedisEnum.BOT.getKey() + receiverId, JSONObject.toJSONString(bot));
            }
        }
        if (bot != null) {
            lastMessageBO.setChatName(bot.getName());
            lastMessageBO.setAvatar(bot.getAvatar());
        } else {
            lastMessageBO.setAvatar("");
            lastMessageBO.setChatName("");
        }

        // 封装 最后消息内容
        lastMessageContent.setContent(messageRequest.getMessage());
        lastMessageBO.setLastMessageContent(lastMessageContent);

        return lastMessageBO;
    }
}
