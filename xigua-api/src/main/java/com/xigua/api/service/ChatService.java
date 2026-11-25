package com.xigua.api.service;

import com.xigua.domain.vo.OpenChatVO;
import com.xigua.domain.vo.UnreadInfoVO;

/**
 * @ClassName ChatService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/12/6 14:42
 */
public interface ChatService {
    OpenChatVO openChat(String chatId, Integer chatType);

    Boolean closeChat(String chatId);

    Boolean hideChat(String chatId);

    /**
     * 获取未读信息
     * @author wangjinfei
     * @date 2026/4/21 20:35
     * @return UnreadInfoVO
    */
    UnreadInfoVO getUnreadInfo();
}
