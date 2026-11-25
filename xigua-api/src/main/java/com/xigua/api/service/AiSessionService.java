package com.xigua.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.dto.CreateSessionRequest;
import com.xigua.domain.dto.PageSessionsDTO;
import com.xigua.domain.entity.AiSession;

import java.util.List;

/**
 * @ClassName AiSessionService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/2/24 21:11
 */
public interface AiSessionService extends IService<AiSession> {
    /**
     * 创建会话
     */
    AiSession createSession(CreateSessionRequest request);

    /**
     * 获取当前登录人有权访问的会话，非本人会话直接拒绝
     */
    AiSession getOwnedSession(String sessionId);

    /**
     * 删除会话
     */
    boolean deleteSession(String sessionId);

    /**
     * 获取会话
     */
    BasePageVO<AiSession> pageSessions(PageSessionsDTO dto);

    /**
     * 重命名会话
     */
    boolean renameSession(String sessionId, String title);

    /**
     * 切换会话模型（供应商）
     */
    boolean updateModel(String sessionId, String model);
}
