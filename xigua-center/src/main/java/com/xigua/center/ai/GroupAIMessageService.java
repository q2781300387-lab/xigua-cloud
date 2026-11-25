package com.xigua.center.ai;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.GroupAIService;
import com.xigua.center.chatMessage.GroupSendChatMessageService;
import com.xigua.common.core.thread.ThreadMgr;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.ChatType;
import com.xigua.domain.enums.ContentType;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.ws.MessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName GroupAIMessageService
 * @Description 群聊 AI 小助手：识别群内 @AI / @小瓜 / @老学究 / /ai 提问，
 *              异步调用 ai 服务生成回复，并以机器人身份把回复发回群聊
 * @Author xigua
 * @Date 2026/11/3 15:35
 */
@Slf4j
@Component
public class GroupAIMessageService {

    /** 触发前缀（不区分大小写） -> 机器人id */
    private static final Map<String, String> TRIGGER_BOT_MAP = new HashMap<>();

    private static final String BOT_XIAOGUA = "243400000000000001";
    private static final String BOT_LAOXUEJIU = "243400000000000002";
    private static final String BOT_XIAOGUA_NAME = "DeepSeek·小瓜";
    private static final String BOT_LAOXUEJIU_NAME = "DeepSeek·老学究";
    private static final String BOT_AVATAR = "http://localhost:9000/xigua/2026/10/09/234354043953086464_755d5e59920c7994b7c3877608f2e3b5.jpg";

    static {
        TRIGGER_BOT_MAP.put("@ai", BOT_XIAOGUA);
        TRIGGER_BOT_MAP.put("@小瓜", BOT_XIAOGUA);
        TRIGGER_BOT_MAP.put("@老学究", BOT_LAOXUEJIU);
        TRIGGER_BOT_MAP.put("/ai", BOT_XIAOGUA);
        // 支持中文全角冒号与空格两种写法
    }

    @Autowired
    private Sequence sequence;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private GroupSendChatMessageService groupSendChatMessageService;
    @DubboReference(timeout = 30000)
    private GroupAIService groupAIService;

    /**
     * 入口：群聊消息落库并发送成功后调用。异步触发 AI 回复，不影响主流程。
     */
    public void tryAnswer(MessageRequest messageRequest) {
        if (messageRequest == null) {
            log.info("[GroupAI] tryAnswer skip: null");
            return;
        }
        log.info("[GroupAI] tryAnswer enter: chatType={}, senderId={}, receiverId={}, message={}",
                messageRequest.getChatType(), messageRequest.getSenderId(),
                messageRequest.getReceiverId(), messageRequest.getMessage());
        if (messageRequest.getChatType() == null
                || !ChatType.GROUP.getType().equals(messageRequest.getChatType())) {
            log.info("[GroupAI] tryAnswer skip: chatType not group, chatType={}", messageRequest.getChatType());
            return;
        }
        // 机器人自己发的消息不再触发，避免循环
        if (BOT_XIAOGUA.equals(messageRequest.getSenderId()) || BOT_LAOXUEJIU.equals(messageRequest.getSenderId())) {
            log.info("[GroupAI] tryAnswer skip: bot self message, senderId={}", messageRequest.getSenderId());
            return;
        }
        String message = messageRequest.getMessage();
        if (StringUtils.isBlank(message)) {
            log.info("[GroupAI] tryAnswer skip: blank message");
            return;
        }
        BotTrigger trigger = parseTrigger(message);
        if (trigger == null) {
            log.info("[GroupAI] tryAnswer skip: no trigger matched, message={}", message);
            return;
        }
        log.info("[GroupAI] trigger matched: botId={}, question={}", trigger.botId, trigger.question);

        String groupId = messageRequest.getReceiverId();
        ThreadMgr.getExecutor().execute(() -> {
            try {
                log.info("[GroupAI] async task start: groupId={}, botId={}, question={}",
                        groupId, trigger.botId, trigger.question);
                String answer = groupAIService.chat(trigger.botId, trigger.question);
                log.info("[GroupAI] async task chat returned: answerLen={}, answer={}",
                        answer == null ? -1 : answer.length(), answer);
                sendAsBot(trigger.botId, groupId, answer);
                log.info("[GroupAI] async task sendAsBot done: groupId={}", groupId);
            } catch (Exception e) {
                log.error("群聊AI小助手处理失败, groupId={}, question={}", groupId, trigger.question, e);
            }
        });
    }

    /**
     * 解析触发前缀：@AI xxx / @小瓜 xxx / @老学究 xxx / /ai xxx
     */
    private BotTrigger parseTrigger(String message) {
        String trimmed = message.trim();
        String lower = trimmed.toLowerCase();
        for (Map.Entry<String, String> entry : TRIGGER_BOT_MAP.entrySet()) {
            String prefix = entry.getKey();
            if (lower.startsWith(prefix)) {
                String question = trimmed.substring(prefix.length())
                        .replaceFirst("^[:：\\s]+", "")
                        .trim();
                if (StringUtils.isBlank(question)) {
                    return null;
                }
                return new BotTrigger(entry.getValue(), question);
            }
        }
        return null;
    }

    /**
     * 以机器人身份把回复发到群聊（落库 + 会话列表 + 实时推送 + 未读）
     */
    private void sendAsBot(String botId, String groupId, String text) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        ensureBotUserCache(botId);

        MessageRequest botRequest = new MessageRequest();
        botRequest.setChatMessageId(sequence.nextNo());
        botRequest.setSenderId(botId);
        botRequest.setReceiverId(groupId);
        botRequest.setMessage(text);
        botRequest.setChatType(ChatType.GROUP.getType());
        botRequest.setContentType(ContentType.TEXT.getValue());
        botRequest.setCreateTime(DateUtil.formatDateTime(LocalDateTime.now(), DateUtil.DATE_TIME_FORMATTER));
        botRequest.setServerReceiveTime(System.currentTimeMillis());

        groupSendChatMessageService.saveChatMessage(botRequest);
        groupSendChatMessageService.lastMessage(botRequest);
        groupSendChatMessageService.chatMessage2Receiver(botRequest);
        groupSendChatMessageService.unreadMessageCount(botRequest);
    }

    /**
     * 预热机器人 USER 缓存，使群聊发送人显示机器人名字与头像
     */
    private void ensureBotUserCache(String botId) {
        String userKey = RedisEnum.USER.getKey() + botId;
        String cached = redisClient.get(userKey);
        if (StringUtils.isNotEmpty(cached)) {
            return;
        }
        String name = BOT_XIAOGUA.equals(botId) ? BOT_XIAOGUA_NAME : BOT_LAOXUEJIU_NAME;
        User botUser = new User();
        botUser.setId(botId);
        botUser.setUsername(name);
        botUser.setAvatar(BOT_AVATAR);
        botUser.setDelFlag(0);
        redisClient.set(userKey, JSONObject.toJSONString(botUser));
    }

    /** 触发解析结果 */
    private static class BotTrigger {
        final String botId;
        final String question;

        BotTrigger(String botId, String question) {
            this.botId = botId;
            this.question = question;
        }
    }
}
