package com.xigua.center.service;

import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.dto.ConversationSettingDTO;
import com.xigua.domain.vo.ConversationSettingVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话个性化设置：置顶与草稿，按用户存 Redis，PC 与手机共用同一份数据。
 */
@Slf4j
@Service
public class ConversationSettingService {

    private static final String KEY_PREFIX = "CONVERSATION_SETTING:";
    private static final int MAX_DRAFT_LENGTH = 5000;

    @Autowired
    private RedisClient redisClient;

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }

    private String field(String chatType, String chatId) {
        return chatType + ":" + chatId;
    }

    public Map<String, ConversationSettingVO> batchGet(String userId, List<String> chatIds) {
        Map<String, ConversationSettingVO> map = new HashMap<>();
        if (!StringUtils.hasText(userId) || chatIds == null || chatIds.isEmpty()) {
            return map;
        }
        for (String chatId : chatIds) {
            if (!StringUtils.hasText(chatId)) continue;
            for (Integer chatType : new Integer[]{1, 2, 3}) {
                Object value = redisClient.hashGet(key(userId), field(chatType.toString(), chatId));
                if (value instanceof ConversationSettingVO) {
                    ConversationSettingVO vo = (ConversationSettingVO) value;
                    map.put(field(chatType.toString(), chatId), vo);
                }
            }
        }
        return map;
    }

    public ConversationSettingVO get(String userId, String chatId, Integer chatType) {
        Object value = redisClient.hashGet(key(userId), field(String.valueOf(chatType), chatId));
        if (value instanceof ConversationSettingVO) {
            return (ConversationSettingVO) value;
        }
        return null;
    }

    public ConversationSettingVO save(String userId, ConversationSettingDTO dto) {
        if (!StringUtils.hasText(dto.getChatId())) {
            throw new BusinessException("会话id不能为空");
        }
        if (dto.getChatType() == null) {
            throw new BusinessException("会话类型不能为空");
        }
        String field = field(dto.getChatType().toString(), dto.getChatId());
        ConversationSettingVO vo = get(userId, dto.getChatId(), dto.getChatType());
        if (vo == null) {
            vo = new ConversationSettingVO();
            vo.setChatId(dto.getChatId());
            vo.setChatType(dto.getChatType());
            vo.setPinned(false);
            vo.setDraft("");
        }
        if (dto.getPinned() != null) {
            vo.setPinned(dto.getPinned());
        }
        if (dto.getDraft() != null) {
            String draft = dto.getDraft();
            if (draft.length() > MAX_DRAFT_LENGTH) {
                draft = draft.substring(0, MAX_DRAFT_LENGTH);
            }
            vo.setDraft(draft);
        }
        redisClient.hashPut(key(userId), field, vo);
        log.info("--------->>>>>> 用户 {} 更新会话设置 {}", userId, field);
        return vo;
    }
}
