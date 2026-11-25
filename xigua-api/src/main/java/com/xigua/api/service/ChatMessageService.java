package com.xigua.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.domain.dto.GetLastMesDTO;
import com.xigua.domain.dto.GetHistoryMes;
import com.xigua.domain.entity.ChatMessage;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.vo.ChatMessageVO;
import com.xigua.domain.vo.LastMessageVO;

/**
 * @ClassName ChatMessageService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/17 19:18
 */
public interface ChatMessageService extends IService<ChatMessage> {
    /**
     * 获取好友最后一条消息
     * @author wangjinfei
     * @date 2025/5/17 19:25
     * @param dto
     * @return List<LastChatVO>
    */
    BasePageVO<LastMessageVO> getLastMes(GetLastMesDTO dto);

    /**
     * 根据发送人和接收人获取最后一条消息
     * @author wangjinfei
     * @date 2025/12/14 17:06
     * @param senderId
     * @param receiverId
     * @return LastMessageVO
    */
    LastMessageVO getLastMes(String senderId, String receiverId);

    /**
     * 添加好友最后一条消息到redis
     * @author wangjinfei
     * @date 2025/12/15 15:51
     * @param senderId
     * @param receiverId
     * @param chatType
     * @return LastMessageVO
    */
    LastMessageVO addLastMes2Redis(String senderId, String receiverId, Integer chatType);

    /**
     * 分页获取好友历史消息
     * @author wangjinfei
     * @date 2025/5/25 10:16
     * @param dto
     * @return BasePageVO<ChatMessageVO>
    */
    BasePageVO<ChatMessageVO> getHistoryMes(GetHistoryMes dto);

    /**
     * 编辑自己发送的文本消息（5 分钟内），并广播给会话参与者
     * @param messageId 消息id
     * @param content 新内容
     */
    void editMessage(String messageId, String content);

    /**
     * 分页查询当前用户的视频通话记录（独立通话记录中心）
     * @param dto 分页与未接筛选
     * @return 通话记录
     */
    BasePageVO<com.xigua.domain.vo.CallRecordVO> getCallRecords(com.xigua.domain.dto.GetCallRecordsDTO dto);

    /**
     * 当前用户的未接来电角标数量（跨设备共用同一已读水位）
     */
    int countUnreadCallRecords();

    /**
     * 把当前用户的通话记录标记为已看到，清空未接角标
     */
    void markCallRecordsRead();

    /**
     * 按当前用户维度删除单条通话记录
     * @param messageId 通话消息id
     */
    void deleteCallRecord(String messageId);

    /**
     * 按当前用户维度清空全部通话记录
     */
    void clearCallRecords();


    /**
     * 获取好友最后一条消息
     * @author wangjinfei
     * @date 2025/8/9 12:11
     * @param senderId
     * @param receiverId
     * @return ChatMessage
    */
    ChatMessage getLastMessage(String senderId, String receiverId);

    /**
     * 根据聊天人和聊天类型 查询最后一条消息下
     * @author wangjinfei
     * @date 2025/12/6 18:47
     * @param chatId
     * @param chatType
     * @return ChatMessage
    */
    ChatMessage getLastMes(String senderId, String chatId, Integer chatType);

    /**
     * 获取今日消息总量
     * @author wangjinfei
     * @date 2026/3/9
     * @return Long
    */
    Long countTodayMessages();

    /**
     * 获取昨日消息总量
     * @author wangjinfei
     * @date 2026/3/9
     * @return Long
    */
    Long countYesterdayMessages();

    /**
     * 获取近7天有群消息的群组ID列表（去重）
     * @author wangjinfei
     * @date 2026/3/9
     * @return List<String>
    */
    java.util.List<String> getActiveGroupIds();

    /**
     * 获取截止昨日的近7天有群消息的群组ID列表（去重）
     * @author wangjinfei
     * @date 2026/3/9
     * @return List<String>
    */
    java.util.List<String> getYesterdayActiveGroupIds();

    /**
     * 获取最近24小时消息发送趋势
     * @author wangjinfei
     * @date 2026/3/9
     * @return List<com.xigua.domain.vo.admin.MessageTrendItemVO>
    */
    java.util.List<com.xigua.domain.vo.admin.MessageTrendItemVO> getMessageTrend24Hours();

    /**
     * 获取总消息数
     * @author wangjinfei
     * @date 2026/3/10
     * @return Long
     */
    Long getTotalMessages();

    /**
     * 获取用户发送的消息总数
     * @author wangjinfei
     * @date 2026/3/10
     * @param userId 用户ID
     * @return Long
     */
    Long getTotalMessagesByUser(String userId);

    /**
     * 获取用户今日发送的消息数
     * @author wangjinfei
     * @date 2026/3/10
     * @param userId 用户ID
     * @return Long
     */
    Long countTodayMessagesByUser(String userId);

    /**
     * 获取用户昨日发送的消息数
     * @author wangjinfei
     * @date 2026/3/10
     * @param userId 用户ID
     * @return Long
     */
    Long countYesterdayMessagesByUser(String userId);

    /**
     * 获取群组消息总数
     * @author wangjinfei
     * @date 2026/3/10
     * @param groupId 群组ID
     * @return Long
     */
    Long countGroupMessages(String groupId);

    /**
     * 获取群成员最后发言时间
     * @author wangjinfei
     * @date 2026/3/10
     * @param groupId 群组ID
     * @param senderIds 发送人ID列表
     * @return java.util.Map<String, java.time.LocalDateTime>
     */
    /**
     * 写入一条群系统消息（成员进群/移出/角色变更等）
     * 系统消息以 messageType=6 落库，前端居中灰条渲染，刷新后仍可见
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param content 展示文案
     * @param operatorId 触发操作的用户id
     * @return String 消息id
     */
    String saveGroupSystemMessage(String groupId, String content, String operatorId);

    /**
     * 获取群成员最后发言时间
     * @author wangjinfei
     * @date 2026/3/10
     * @param groupId 群组ID
     * @param senderIds 发送人ID列表
     * @return java.util.Map<java.lang.String, java.time.LocalDateTime>
     */
    java.util.Map<String, java.time.LocalDateTime> getGroupMemberLastMessageTimes(String groupId, java.util.List<String> senderIds);
}
