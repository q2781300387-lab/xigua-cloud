package com.xigua.ai.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.ai.domain.enums.RoleType;
import com.xigua.ai.mapper.AiMemoryMapper;
import com.xigua.api.service.AiMemoryService;
import com.xigua.api.service.AiMessageService;
import com.xigua.api.service.BotService;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.bo.AiMemoryBO;
import com.xigua.domain.entity.*;
import com.xigua.domain.enums.RedisEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @ClassName AiMemoryServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/27 21:04
 */
@Service
public class AiMemoryServiceImpl extends ServiceImpl<AiMemoryMapper, AiMemory> implements AiMemoryService {
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private AiMessageService aiMessageService;
    @Autowired
    private BotService botService;

    @Override
    public List<AiMemoryBO> refreshMemory(String sessionId, int size) {
        List<AiMemoryBO> memories = buildMemory(sessionId, size);
        // todo 添加到redis
        return memories;
    }

    private List<AiMemoryBO> buildMemory(String sessionId, int size) {
        List<AiMemoryBO> aiMemoryBO = new ArrayList<>();
        AiSession aiSession = redisClient.get(RedisEnum.AI_SESSION.getKey() + sessionId, AiSession.class);
        if (aiSession == null) {
            return aiMemoryBO;
        }
        Bot bot = redisClient.get(RedisEnum.BOT.getKey() + aiSession.getBotId(), Bot.class);
        if (bot == null) {
            // 缓存丢失时 bot 为 null，下面 getPrompt() 直接抛空指针，
            // 整轮对话的记忆都组不出来，这里回查数据库并回填
            bot = botService.getById(aiSession.getBotId());
            if (bot == null) {
                return aiMemoryBO;
            }
            redisClient.set(RedisEnum.BOT.getKey() + aiSession.getBotId(), JSONObject.toJSONString(bot));
        }

        AiMemoryBO system = new AiMemoryBO();
        system.setRole(RoleType.SYSTEM.getRoleName());
        system.setContent(bot.getPrompt());
        aiMemoryBO.add(system);

        List<AiMessage> aiMessages = aiMessageService.lastMessages(sessionId, size - 1);
        if(CollectionUtils.isEmpty(aiMessages)){
            return aiMemoryBO;
        }

        aiMessages.forEach(message -> {
            AiMemoryBO message2 = new AiMemoryBO();
            message2.setRole(message.getRole());
            message2.setContent(message.getContent());
            aiMemoryBO.add(message2);
        });

        return aiMemoryBO;
    }
}
