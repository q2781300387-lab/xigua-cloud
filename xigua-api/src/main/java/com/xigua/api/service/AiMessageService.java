package com.xigua.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.dto.GetAiHistoryMes;
import com.xigua.domain.entity.AiMessage;
import com.xigua.domain.vo.AiHistoryMessageVO;

import java.util.List;

/**
 * @ClassName AiMessageService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/2/24 21:13
 */
public interface AiMessageService extends IService<AiMessage> {
    void saveUserMessage(String sessionId, String input);
    void saveAiMessage(String sessionId, String input);
    void saveSystemMessage(String sessionId, String input);
    List<AiMessage> lastMessages(String sessionId, int size);
    BasePageVO<AiHistoryMessageVO> pageMessages(GetAiHistoryMes dto);
}
