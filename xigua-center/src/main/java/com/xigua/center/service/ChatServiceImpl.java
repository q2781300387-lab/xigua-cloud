package com.xigua.center.service;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.CenterService;
import com.xigua.api.service.ChatMessageService;
import com.xigua.api.service.ChatService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.model.UserToken;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.bo.LastMessageContentBO;
import com.xigua.domain.entity.ChatMessage;
import com.xigua.domain.entity.Group;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.ChatType;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.vo.LastMessageVO;
import com.xigua.domain.vo.OpenChatVO;
import com.xigua.domain.vo.UnreadInfoVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ClassName ChatServiceImpl
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/12/6 14:43
 */
@Service
public class ChatServiceImpl implements ChatService {
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private ChatMessageService chatMessageService;


    @Override
    public OpenChatVO openChat(String chatId, Integer chatType) {
        if(StringUtils.isEmpty(chatId)){
            throw new BusinessException("聊天id不能为空");
        }
        if (chatType == null){
            throw new BusinessException("聊天类型不能为空");
        }

        OpenChatVO vo = new OpenChatVO();
        vo.setRank(0L);
        UserToken userToken = UserContext.get();
        String userId = userToken.getUserId();

        // 如果redis存在和chatId最后聊天消息  直接退出
        if(redisClient.isExistInZSet(RedisEnum.LAST_MES.getKey() + userId, chatId)){
            LastMessageVO lastMes = chatMessageService.getLastMes(userId, chatId);
            BeanUtils.copyProperties(lastMes, vo);

            // 同步redis 当前聊天窗口
            redisClient.set(RedisEnum.CURRENT_CHAT_WINDOW.getKey() + userId, chatId);
            // 获取当前聊天窗口排名
            Long rank = redisClient.zrerank(RedisEnum.LAST_MES.getKey() + userId, chatId);
            if(rank != null){
                vo.setRank(rank);
            }
            return vo;
        }

        ChatMessage lastMessage = chatMessageService.getLastMes(userId, chatId, chatType);
        if(lastMessage == null){
            vo.setChatId(chatId);
            vo.setRank(1L);
        }

        // 同步redis 最后聊天消息
        LastMessageVO lastMessageVO = chatMessageService.addLastMes2Redis(userId, chatId, chatType);
        BeanUtils.copyProperties(lastMessageVO, vo);

        // 获取当前聊天窗口排名
        if(vo.getRank() != null){
            Long rank = redisClient.zrerank(RedisEnum.LAST_MES.getKey() + userId, chatId);
            if(rank != null){
                vo.setRank(rank);
            }
        }

        // 同步redis 当前聊天窗口
        redisClient.set(RedisEnum.CURRENT_CHAT_WINDOW.getKey() + userId, chatId);

        return vo;
    }

    @Override
    public Boolean closeChat(String chatId) {
        UserToken userToken = UserContext.get();
        String userId = userToken.getUserId();

        // 缓存删除当前聊天窗口
        String currentChat = redisClient.get(RedisEnum.CURRENT_CHAT_WINDOW.getKey() + userId);
        if(StringUtils.isNotEmpty(currentChat) && currentChat.equals(chatId)){
            redisClient.del(RedisEnum.CURRENT_CHAT_WINDOW.getKey() + userId);
        }

        return true;
    }

    @Override
    public Boolean hideChat(String chatId) {
        UserToken userToken = UserContext.get();
        String userId = userToken.getUserId();

        // 缓存删除消息列表
        if(redisClient.isExistInZSet(RedisEnum.LAST_MES.getKey() + userId, chatId)){
            redisClient.zsrem(RedisEnum.LAST_MES.getKey() + userId, chatId);
        }

        // 缓存删除当前聊天窗口
        String currentChat = redisClient.get(RedisEnum.CURRENT_CHAT_WINDOW.getKey() + userId);
        if(StringUtils.isNotEmpty(currentChat) && currentChat.equals(chatId)){
            redisClient.del(RedisEnum.CURRENT_CHAT_WINDOW.getKey() + userId);
        }

        return true;
    }

    @Override
    public UnreadInfoVO getUnreadInfo() {
        UserToken userToken = UserContext.get();
        String userId = userToken.getUserId();

        UnreadInfoVO vo = new UnreadInfoVO();

        // 获取总未读数量
        String chatUnreadCountKey = RedisEnum.CHAT_UNREAD_COUNT.getKey() + userId;
        Long chatUnreadCount = redisClient.getLong(chatUnreadCountKey);
        vo.setChatUnreadCount(chatUnreadCount != null ? chatUnreadCount : 0L);

        return vo;
    }
}
