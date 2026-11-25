package com.xigua.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.common.mybatis.base.PagePlus;
import com.xigua.domain.entity.GroupMember;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * @ClassName GroupMemberService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/7/6 11:14
 */
public interface GroupMemberService extends IService<GroupMember> {

    /**
     * 添加群成员
     * @author wangjinfei
     * @date 2025/7/6 11:27
     * @param groupId
     * @param memberIds
     * @return Boolean
    */
    Boolean addGroupMember(String groupId, List<String> memberIds);

    /**
     * 获取群成员数量
     * @author wangjinfei
     * @date 2025/7/27 9:42
     * @param userId
     * @return Integer
     */
    Integer getCountByUserId(String userId);

    /**
     * 获取加入的群列表
     * @author wangjinfei
     * @date 2025/7/27 11:08
     * @param userId
     * @return Set<String>
    */
    Set<String> getGroupIdsByUserId(String userId);

    /**
     * 获取群成员列表
     * @author wangjinfei
     * @date 2025/7/27 11:21
     * @param groupId
     * @return List<GroupMember>
    */
    List<GroupMember> getListByGroupId(String groupId);

    /**
     * 获取加入时间
     * @author wangjinfei
     * @date 2025/7/30 19:46
     * @param groupId
     * @param userId
     * @return LocalDateTime
    */
    LocalDateTime getJoinTime(String groupId, String userId);

    /**
     * 获取群角色
     * @author wangjinfei
     * @date 2025/7/30 19:59
     * @param groupId
     * @param userId
     * @return Integer
    */
    Integer getGroupRole(String groupId, String userId);

    /**
     * 根据群id获取群成员列表
     * @author wangjinfei
     * @date 2025/8/17 17:47
     * @param groupId
     * @return Set<String>
    */
    Set<String> getGroupMembers(String groupId);

    /**
     * 群成员分页查询
     * @author wangjinfei
     * @date 2026/3/10
     * @param groupId
     * @param keyword
     * @param pageNum
     * @param pageSize
     * @return PagePlus<GroupMember>
     */
    PagePlus<GroupMember> pageByGroupId(String groupId, String keyword, Integer pageNum, Integer pageSize);

    /**
     * 移除单个群成员（退出群聊时调用）
     * @author doubao
     * @date 2026/11/6
     * @param groupId 群组id
     * @param userId 用户id
     * @return Boolean 是否移除成功
     */
    Boolean removeGroupMember(String groupId, String userId);

    /**
     * 修改群成员角色（设/取消管理员）
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param userId 目标用户id
     * @param role 角色（1：群主；2：群管理；3：普通成员）
     * @return Boolean 是否修改成功
     */
    Boolean updateRole(String groupId, String userId, Integer role);

    /**
     * 更新成员单人禁言截止时间（null 表示解除）
     * @param groupId 群组id
     * @param userId 目标用户id
     * @param muteUntil 截止时间
     * @return Boolean 是否更新成功
     */
    Boolean updateMuteUntil(String groupId, String userId, LocalDateTime muteUntil);

    /**
     * 查询成员单人禁言截止时间
     * @param groupId 群组id
     * @param userId 目标用户id
     * @return LocalDateTime 截止时间，未禁言返回 null
     */
    LocalDateTime getMuteUntil(String groupId, String userId);

    /**
     * 移除群内全部成员（解散群聊时调用）
     * @author doubao
     * @date 2026/11/6
     * @param groupId 群组id
     * @return Boolean 是否移除成功
     */
    Boolean removeAllGroupMember(String groupId);
}
