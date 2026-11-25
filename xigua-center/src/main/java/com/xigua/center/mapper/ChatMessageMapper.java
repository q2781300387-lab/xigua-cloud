package com.xigua.center.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xigua.domain.bo.ConversationPeerBO;
import com.xigua.domain.entity.ChatMessage;
import com.xigua.domain.entity.Group;
import com.xigua.domain.vo.ChatMessageSyncVO;
import com.xigua.domain.vo.ChatMessageVO;
import com.xigua.domain.vo.CallRecordVO;
import com.xigua.domain.vo.LastMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @ClassName Message
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/17 19:16
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    /**
     * 按发送人 + 客户端消息id查询，用于重发幂等
     */
    @org.apache.ibatis.annotations.Select("select * from xg_chat_message " +
            "where sender_id = #{senderId} and client_msg_id = #{clientMsgId} and del_flag = 0 limit 1")
    ChatMessage getByClientMsgId(@Param("senderId") String senderId, @Param("clientMsgId") String clientMsgId);

    /**
     * 获取好友最后一条消息
     * @author wangjinfei
     * @date 2025/5/19 21:26
     * @param page
     * @return List<LastChatVO>
    */
    List<LastMessageVO> getLastMes(Page<LastMessageVO> page, @Param("userId") String userId);

    /**
     * 分页获取好友历史消息
     * @author wangjinfei
     * @date 2025/5/25 11:01
     * @param page
     * @param senderId
     * @param receiverId
     * @return List<ChatMessageVO>
    */
    List<ChatMessageVO> getPrivateChatHistoryMes(Page<ChatMessageVO> page, @Param("senderId") String senderId,
                                      @Param("receiverId") String receiverId);

    /**
     * 获取好友最后一条消息
     * @author wangjinfei
     * @date 2025/8/9 12:11
     * @param senderId
     * @param receiverId
     * @return ChatMessage
     */
    ChatMessage getLastMessage(@Param("senderId") String senderId, @Param("receiverId") String receiverId);

    /**
     * 分页获取群聊历史消息
     * @author wangjinfei
     * @date 2025/8/17 20:16
     * @param page
     * @param userId 当前用户id
     * @param groupId 群聊id
     * @return List<ChatMessageVO>
    */
    List<ChatMessageVO> getGroupChatHistoryMes(Page<ChatMessageVO> page, @Param("userId") String userId,
                                               @Param("groupId") String groupId);

    /**
     * 分页获取机器人历史消息
     * @author wangjinfei
     * @date 2026/1/25 19:10
     * @param page
     * @param userId
     * @param botId
     * @return List<ChatMessageVO>
    */
    List<ChatMessageVO> getBotChatHistoryMes(Page<ChatMessageVO> page, @Param("userId") String userId,
                                              @Param("botId") String botId);

    /**
     * 查询最后一个好友消息
     * @author wangjinfei
     * @date 2025/12/6 18:44
     * @param senderId
     * @param receiverId
     * @return ChatMessage
    */
    ChatMessage getPrivateLastMes(@Param("senderId") String senderId, @Param("receiverId") String receiverId);

    /**
     * 查询最后一条群聊消息
     * @author wangjinfei
     * @date 2025/12/6 18:44
     * @param groupId
     * @return ChatMessage
    */
    ChatMessage getGroupLastMes(@Param("groupId") String groupId);

    /**
     * 获取群成员最后发言时间
     * @author wangjinfei
     * @date 2026/3/10
     * @param groupId
     * @param senderIds
     * @return List<com.xigua.domain.vo.admin.GroupMemberLastMessageVO>
     */
    java.util.List<com.xigua.domain.vo.admin.GroupMemberLastMessageVO> getGroupMemberLastMessageTimes(
        @Param("groupId") String groupId,
        @Param("senderIds") java.util.List<String> senderIds
    );

    /**
     * 按 id 升序游标分页查询聊天消息（用于全量同步到搜索引擎）
     * @author wangjinfei
     * @date 2026/9/29
     * @param lastId 上一批最后一条消息id，首次传 null
     * @param pageSize 每批数量
     * @return List<ChatMessageSyncVO>
    */
    List<ChatMessageSyncVO> pageByIdAsc(@Param("lastId") String lastId, @Param("pageSize") int pageSize);

    /**
     * 按 createTime + id 双游标分页查询聊天消息（用于增量对账补齐 es 漏掉的数据）
     * @author wangjinfei
     * @date 2026/11/12
     * @param afterTime 起始时间，只查该时间之后的消息
     * @param lastId 上一批最后一条消息id，首次传 null
     * @param pageSize 每批数量
     * @return List<ChatMessageSyncVO>
    */
    List<ChatMessageSyncVO> pageByCreateTimeAsc(@Param("afterTime") String afterTime,
                                                @Param("lastId") String lastId,
                                                @Param("pageSize") int pageSize);

    /**
     * 查询某人未读且被 @ 的会话 id（单聊返回对方id，群聊返回群id）
     * 用于离线期间补拉「@我」提醒
     * @param userId 当前用户id
     * @return 会话id列表
     */
    @org.apache.ibatis.annotations.Select(
            "SELECT DISTINCT m.receiver_id FROM xg_chat_message m " +
            "JOIN xg_message_read r ON r.message_id = m.id " +
            "WHERE r.receiver_id = #{userId} AND r.is_read = 0 AND r.del_flag = 0 " +
            "AND m.del_flag = 0 AND m.sender_id <> #{userId} " +
            "AND m.at_users IS NOT NULL " +
            "AND m.at_users LIKE CONCAT('%\"', #{userId}, '\"%') " +
            "LIMIT 100")
    java.util.List<String> selectAtMeChatIds(@Param("userId") String userId);

    /**
     * 每个会话（单聊对端 / 群 / 机器人）的最新一条有效消息。
     * 会话列表原本只读 Redis，缓存被清空或消息是直接写库导入的就会整片丢失，用它回源重建。
     * @param userId 当前用户id
     * @return 每个会话最新一条
     */
    List<ConversationPeerBO> selectLatestPerPeer(@Param("userId") String userId);

    /**
     * 群名与群头像。会话列表缓存整体失效时用来补群聊主体信息，
     * 避免为了这点信息去引入跨模块的 Dubbo 依赖
     * @param groupId 群id
     * @return Group（只填 id / groupName / groupAvatar / currentMember）
     */
    Group selectGroupMeta(@Param("groupId") String groupId);

    /**
     * 分页查询当前用户的单聊视频通话记录。
     * messageType=9 是新版视频通话卡片；旧版 contentType=6 的通话卡片也兼容进来，
     * 但必须限定单聊且消息体含 kind=video，避免混入群系统提示。
     * @param page 分页
     * @param userId 当前用户
     * @param missedOnly 是否只看未接
     * @param keyword 对端昵称关键词
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @return 通话记录
     */
    List<CallRecordVO> getCallRecords(Page<CallRecordVO> page,
                                      @Param("userId") String userId,
                                      @Param("missedOnly") boolean missedOnly,
                                      @Param("keyword") String keyword,
                                      @Param("startTime") String startTime,
                                      @Param("endTime") String endTime);

    /**
     * 未接来电角标：只看呼入且未读水位之后的 rejected/timeout/busy
     */
    int countUnreadCallRecords(@Param("userId") String userId);

    /**
     * 更新当前用户的通话记录已读水位
     */
    int markCallRecordsRead(@Param("userId") String userId);

    /**
     * 按用户维度隐藏一条通话记录，不影响对端
     */
    int deleteCallRecordForUser(@Param("userId") String userId,
                                @Param("messageId") String messageId);

    /**
     * 按用户维度清空全部通话记录，不影响对端
     */
    int clearCallRecordsForUser(@Param("userId") String userId);
}
