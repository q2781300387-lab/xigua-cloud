package com.xigua.api.service;

import com.xigua.domain.dto.GroupDTO;
import com.xigua.domain.entity.Group;
import com.xigua.domain.vo.GroupDetailVO;
import com.xigua.domain.vo.UserSearchVO;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName GroupService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/7/6 10:59
 */
public interface GroupService {

    /**
     * 根据主键获取群组（管理员端使用，显式暴露，避免继承 IService 撑爆 Dubbo 元数据）
     * @param id 群组id
     * @return Group
     */
    Group getById(Serializable id);

    /**
     * 创建群组
     * @author wangjinfei
     * @date 2025/7/6 11:07
     * @param dto
     * @return Boolean
    */
    Boolean createGroup(GroupDTO dto) throws Exception;

    /**
     * 群组添加到缓存
     * @author wangjinfei
     * @date 2025/7/28 15:28
     * @param groupId
     * @return Boolean
    */
    Boolean addGroup2Redis(String groupId);

    /**
     * 添加最后一条空消息到缓存
     * @author wangjinfei
     * @date 2025/8/23 13:15
     * @param groupId
     * @return Boolean
    */
    Boolean addLastEmptyMes2Redis(String groupId);

    /**
     * 获取所有群组id
     * @author wangjinfei
     * @date 2025/7/29 17:50
     * @return List<String>
     */
    List<String> getAllGroupId();

    /**
     * 获取群组详情
     * @author wangjinfei
     * @date 2025/7/30 19:35
     * @param groupId
     * @return GroupDetailVO
    */
    GroupDetailVO getGroupDetail(String groupId);

    /**
     * 获取群成员用户信息列表（@提及选人用）
     * @author doubao
     * @date 2026/11/4
     * @param groupId 群组id
     * @return List<UserSearchVO>
    */
    List<UserSearchVO> getGroupMemberUsers(String groupId);

    /**
     * 根据群组id获取群组
     * @author wangjinfei
     * @date 2025/8/17 9:50
     * @return List<Group>
    */
    List<Group> getListByIds(List<String> groupIds);

    /**
     * 获取所有群组
     * @author wangjinfei
     * @date 2025/8/17 9:55
     * @return List<Group>
    */
    List<Group> getAllGroup();

    /**
     * 分页查询群组
     * @author wangjinfei
     * @date 2026/3/7
     * @param groupName 群组名称
     * @param ownerName 群主名称
     * @param createTime 创建时间
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return Page<Group>
    */
    com.xigua.common.mybatis.base.PagePlus<Group> page(String groupName, String ownerName, String createTime, Integer pageNum, Integer pageSize);

    /**
     * 获取近7天活跃群组数（有消息的群组）
     * @author wangjinfei
     * @date 2026/3/9
     * @return Long
    */
    Long countActiveGroups();

    /**
     * 获取昨日7天活跃群组数（用于计算增长率）
     * @author wangjinfei
     * @date 2026/3/9
     * @return Long
    */
    Long countYesterdayActiveGroups();

    /**
     * 获取活跃群组排行（按成员数降序，前5名）
     * @author wangjinfei
     * @date 2026/3/9
     * @return List<com.xigua.domain.vo.admin.ActiveGroupRankItemVO>
    */
    java.util.List<com.xigua.domain.vo.admin.ActiveGroupRankItemVO> getActiveGroupRank();

    /**
     * 退出群聊（群主不可退出，需解散）
     * @author doubao
     * @date 2026/11/6
     * @param groupId 群组id
     * @return Boolean 是否退出成功
     */
    Boolean quitGroup(String groupId);

    /**
     * 解散群聊（仅群主可操作）
     * @author doubao
     * @date 2026/11/6
     * @param groupId 群组id
     * @return Boolean 是否解散成功
     */
    Boolean disbandGroup(String groupId);

    /**
     * 邀请成员入群（群主或管理员可操作）
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param userIds 被邀请用户id列表
     * @return Integer 实际入群人数
     */
    Integer inviteMembers(String groupId, List<String> userIds);

    /**
     * 将成员移出群聊（群主可移出任何人，管理员只能移出普通成员）
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param targetUserId 被移出用户id
     * @return Boolean 是否移出成功
     */
    Boolean kickMember(String groupId, String targetUserId);

    /**
     * 设置成员角色（仅群主；2=管理员，3=普通成员）
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param targetUserId 目标用户id
     * @param role 目标角色
     * @return Boolean 是否设置成功
     */
    Boolean setMemberRole(String groupId, String targetUserId, Integer role);

    /**
     * 修改群资料（群名称 / 群头像 / 群简介；群主或管理员可操作）
     * @param dto 群资料
     * @return Boolean 是否修改成功
     */
    Boolean updateGroupInfo(com.xigua.domain.dto.GroupUpdateDTO dto);

    /**
     * 发布 / 编辑群公告（群主或管理员可操作）
     * @param groupId 群组id
     * @param announcement 公告内容
     * @return Boolean 是否保存成功
     */
    Boolean updateAnnouncement(String groupId, String announcement);

    /**
     * 开启 / 关闭全员禁言（群主或管理员可操作）
     * @param groupId 群组id
     * @param muteAll 0：关闭；1：开启
     * @return Boolean 是否保存成功
     */
    Boolean updateMuteAll(String groupId, Integer muteAll);

    /**
     * 单人禁言 / 解除禁言（群主可禁言管理员与成员，管理员只能禁言普通成员）
     * @param groupId 群组id
     * @param targetUserId 目标成员
     * @param durationMinutes 禁言分钟数；0 表示解除禁言
     * @return Boolean 是否成功
     */
    Boolean muteMember(String groupId, String targetUserId, Integer durationMinutes);

    /**
     * 转让群主（仅群主可操作）
     * @param groupId 群组id
     * @param targetUserId 新群主id
     * @return Boolean 是否转让成功
     */
    Boolean transferOwner(String groupId, String targetUserId);

    /**
     * 管理端设置全员禁言（不依赖当前登录用户，仅由后台调用）
     */
    Boolean adminSetMuteAll(String groupId, Integer muteAll);

    /**
     * 管理端解散群聊（不依赖当前登录用户，仅由后台调用）
     */
    Boolean adminDisbandGroup(String groupId);

    /**
     * 管理端移出群成员（不依赖当前登录用户，仅由后台调用）
     */
    Boolean adminRemoveMember(String groupId, String targetUserId);

    /**
     * 按群号/群名称搜索群聊
     */
    java.util.List<com.xigua.domain.vo.GroupJoinRequestVO> searchGroups(String keyword);

    /**
     * 提交入群申请
     */
    Boolean requestJoin(String groupId, String reason);

    /**
     * 查询某群的待审批入群申请（群主/管理员）
     */
    java.util.List<com.xigua.domain.vo.GroupJoinRequestVO> listJoinRequests(String groupId);

    /**
     * 审批入群申请（群主/管理员）
     */
    Boolean handleJoinRequest(String requestId, Boolean approve);

    /** 发起群通话（4 人以内 mesh） */
    com.xigua.domain.vo.GroupCallRoomVO startGroupCall(String groupId, Boolean video);

    /** 加入进行中的群通话 */
    com.xigua.domain.vo.GroupCallRoomVO joinGroupCall(String groupId, String callId);

    /** 查询当前群通话状态 */
    com.xigua.domain.vo.GroupCallRoomVO getGroupCallState(String groupId);

    /** 离会 */
    Boolean leaveGroupCall(String groupId, String callId);

    /** 发送 WebRTC 信令 */
    Boolean sendGroupCallSignal(com.xigua.domain.dto.GroupCallSignalDTO dto);

    /** 拉取并清空本端信令 */
    java.util.List<com.xigua.domain.vo.GroupCallSignalVO> pollGroupCallSignals(String groupId, String callId);
}
