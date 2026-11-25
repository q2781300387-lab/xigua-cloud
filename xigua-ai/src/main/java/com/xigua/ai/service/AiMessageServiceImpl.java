package com.xigua.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.ai.domain.enums.RoleType;
import com.xigua.ai.mapper.AiMessageMapper;
import com.xigua.api.service.AiMessageService;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.dto.GetAiHistoryMes;
import com.xigua.domain.entity.AiMessage;
import com.xigua.domain.util.BasePage;
import com.xigua.domain.vo.AiHistoryMessageVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @ClassName AiMessageServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/24 21:16
 */
@Service
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage> implements AiMessageService {
    @Autowired
    private Sequence sequence;
    @Autowired
    private RedisClient redisClient;

    @Override
    public void saveUserMessage(String sessionId, String input) {
        AiMessage aiMessage = new AiMessage();
        aiMessage.setId(sequence.nextNo());
        aiMessage.setSessionId(sessionId);
        aiMessage.setContent(input);
        aiMessage.setRole(RoleType.USER.getRoleName());
        baseMapper.insert(aiMessage);
    }

    @Override
    public void saveAiMessage(String sessionId, String input) {
        AiMessage aiMessage = new AiMessage();
        aiMessage.setId(sequence.nextNo());
        aiMessage.setSessionId(sessionId);
        aiMessage.setContent(input);
        aiMessage.setRole(RoleType.ASSISTANT.getRoleName());
        baseMapper.insert(aiMessage);
    }

    @Override
    public void saveSystemMessage(String sessionId, String input) {
        AiMessage aiMessage = new AiMessage();
        aiMessage.setId(sequence.nextNo());
        aiMessage.setSessionId(sessionId);
        aiMessage.setContent(input);
        aiMessage.setRole(RoleType.SYSTEM.getRoleName());
        baseMapper.insert(aiMessage);
    }

    @Override
    public List<AiMessage> lastMessages(String sessionId, int size) {
        return baseMapper.lastMessages(sessionId, size);
    }

    @Override
    public BasePageVO<AiHistoryMessageVO> pageMessages(GetAiHistoryMes dto) {
        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        Page<AiHistoryMessageVO> page = new Page<>(pageNum, pageSize);
        List<AiHistoryMessageVO> aiHistoryMessages = baseMapper.pageMessages(page, dto);
        if(CollectionUtils.isEmpty(aiHistoryMessages)){
            return BasePage.emptyResult(page);
        }

        // 反转消息列表 页面显示最新的消息在最下面
        Collections.reverse(aiHistoryMessages);
        return BasePage.getResult(page, aiHistoryMessages);
    }
}
