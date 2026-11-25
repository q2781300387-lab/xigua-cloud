package com.xigua.client.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.client.mapper.GroupMemberMapper;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.mybatis.base.PagePlus;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.entity.GroupMember;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.GroupRole;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.api.service.GroupMemberService;
import com.xigua.api.service.UserService;
import com.xigua.domain.vo.UserSearchVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @ClassName GroupMemberServiceImpl
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/7/6 11:14
 */
@Slf4j
@Service
@DubboService
public class GroupMemberServiceImpl extends ServiceImpl<GroupMemberMapper, GroupMember> implements GroupMemberService {
    @Autowired
    private Sequence sequence;
    @Autowired
    private RedisClient redisClient;
    @DubboReference
    private UserService userService;

    /**
     * 添加群成员
     * @author wangjinfei
     * @date 2025/7/6 11:27
     * @param groupId
     * @param memberIds
     * @return Boolean
     */
    @Override
    public Boolean addGroupMember(String groupId, List<String> memberIds) {
        List<GroupMember> groupMemberList = new ArrayList<>();

        for (String memberId : memberIds) {
            GroupMember groupMember = new GroupMember();
            groupMember.setId(sequence.nextNo());
            groupMember.setGroupId(groupId);
            groupMember.setUserId(memberId);

            // 设置群内角色
            groupMember.setRole(setGroupRole(memberId));

            // 设置群内昵称
            groupMember.setNickname(setNickname(memberId));

            groupMemberList.add(groupMember);
        }

        // 批量插入
        baseMapper.insert(groupMemberList);

        // 同步群成员 zset：原来只写库不写缓存，后加入的成员不在缓存里，
        // 会收不到群消息、已读记录建不出来、已读人数也少算
        String memberKey = RedisEnum.GROUP_MEMBER_ID.getKey() + groupId;
        long score = System.currentTimeMillis();
        for (GroupMember groupMember : groupMemberList) {
            redisClient.zsadd(memberKey, groupMember.getUserId(), score);
        }

        return true;
    }

    /**
     * 设置群内角色
     * @author wangjinfei
     * @date 2025/7/6 11:42
     * @param memberId
     * @return Integer
    */
    private Integer setGroupRole(String memberId){
        String userId = UserContext.get().getUserId();

        if (memberId.equals(userId)) { // 是当前用户为群主
            return GroupRole.OWNER.getType();
        } else { // 其他默认普通成员
            return GroupRole.MEMBER.getType();
        }
    }

    /**
     * 设置群内昵称
     * @author wangjinfei
     * @date 2025/7/6 11:52
     * @param memberId
     * @return String
    */
    private String setNickname(String memberId){
        // 创建群成员时，默认使用用户名作为群内昵称

        // 缓存中获取用户名
        String userCache = redisClient.get(RedisEnum.USER.getKey() + memberId);
        if(StringUtils.isNotEmpty(userCache)){
            User user = JSONObject.parseObject(userCache, User.class);
            if(user != null && StringUtils.isNotEmpty(user.getUsername())){
                return user.getUsername();
            }
        }

        // 缓存中没有，查询数据库
        User user = userService.getById(memberId);
        if(user != null && StringUtils.isNotEmpty(user.getUsername())){
            // 可以再次 缓存用户信息
//            redisClient.set(RedisEnum.USER.getKey() + memberId, JSONObject.toJSONString(user));
            return user.getUsername();
        }

        return null;
    }

    /**
     * 获取群成员数量
     * @author wangjinfei
     * @date 2025/7/27 9:42
     * @param userId
     * @return Integer
     */
    @Override
    public Integer getCountByUserId(String userId) {
        Integer count = baseMapper.getCountByUserId(userId);
        return count == null ? 0 : count;
    }

    /**
     * 获取加入的群列表
     * @author wangjinfei
     * @date 2025/7/27 11:08
     * @param userId
     * @return Set<String>
     */
    @Override
    public Set<String> getGroupIdsByUserId(String userId) {
        Set<String> groupIds = redisClient.scanZSetKeysInMember(userId, RedisEnum.GROUP_MEMBER_ID.getKey() + "*")
                .stream()
                .map(key -> key.replace(RedisEnum.GROUP_MEMBER_ID.getKey(), "")) // 去除前缀
                .collect(Collectors.toSet());
        // zset 只在“走接口进群”时写入，直接写库导入的成员关系不会进缓存，Redis 被清空或部分清理后也会丢。
        // 以数据库的成员关系为准补齐缓存：只判断“整体为空”会漏掉部分缺失的情况，
        // 表现为群成员接口能看到、首页群数量也对得上，群列表却少一个
        List<GroupMember> dbMembers = baseMapper.selectList(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getStatus, 1)
                .eq(GroupMember::getDelFlag, 0));
        if (CollectionUtils.isEmpty(dbMembers)) {
            return groupIds;
        }
        for (GroupMember member : dbMembers) {
            String groupId = member.getGroupId();
            if (StringUtils.isEmpty(groupId) || groupIds.contains(groupId)) {
                continue;
            }
            groupIds.add(groupId);
            long joinTime = member.getCreateTime() == null
                    ? System.currentTimeMillis()
                    : DateUtil.toEpochMilli(member.getCreateTime());
            redisClient.zsadd(RedisEnum.GROUP_MEMBER_ID.getKey() + groupId, userId, joinTime);
        }

        return groupIds;
    }

    /**
     * 获取群成员列表
     * @author wangjinfei
     * @date 2025/7/27 11:21
     * @param groupId
     * @return List<GroupMember>
     */
    @Override
    public List<GroupMember> getListByGroupId(String groupId) {
        List<GroupMember> groupMembers = baseMapper.selectList(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId));
        if(CollectionUtils.isEmpty(groupMembers)){
            return List.of();
        }

        return groupMembers;
    }

    /**
     * 获取加入时间
     * @author wangjinfei
     * @date 2025/7/30 19:46
     * @param groupId
     * @param userId
     * @return LocalDateTime
     */
    @Override
    public LocalDateTime getJoinTime(String groupId, String userId) {
        if(StringUtils.isEmpty(groupId) || StringUtils.isEmpty(userId)){
            throw new BusinessException("群ID或用户ID不能为空");
        }

        long score = redisClient.getScore(RedisEnum.GROUP_MEMBER_ID.getKey() + groupId, userId);
        if(score == 0){
            // 缓存缺失时回源数据库，避免 Redis 被清空后老成员被误判为“未加入群聊”
            GroupMember member = getActiveMemberFromDb(groupId, userId);
            if(member == null || member.getCreateTime() == null){
                throw new BusinessException("用户未加入群聊");
            }
            score = DateUtil.toEpochMilli(member.getCreateTime());
            redisClient.zsadd(RedisEnum.GROUP_MEMBER_ID.getKey() + groupId, userId, score);
        }

        LocalDateTime localDateTime = DateUtil.toLocalDateTime(score);
        return localDateTime;
    }

    /**
     * 获取群角色
     * @author wangjinfei
     * @date 2025/7/30 19:59
     * @param groupId
     * @param userId
     * @return Integer
     */
    @Override
    public Integer getGroupRole(String groupId, String userId) {
        if(StringUtils.isEmpty(groupId) || StringUtils.isEmpty(userId)){
            throw new BusinessException("群ID或用户ID不能为空");
        }

        String groupMemberCache = redisClient.get(RedisEnum.GROUP_MEMBER.getKey() + groupId + "_" + userId);
        GroupMember groupMember = null;
        if(StringUtils.isNotEmpty(groupMemberCache)){
            groupMember = JSONObject.parseObject(groupMemberCache, GroupMember.class);
        }
        // 缓存命中也要认状态：被移除/已退群的旧缓存不能继续当成有效成员
        if(groupMember != null && ((groupMember.getDelFlag() != null && groupMember.getDelFlag() == 1)
                || (groupMember.getStatus() != null && groupMember.getStatus() == 0))){
            redisClient.del(RedisEnum.GROUP_MEMBER.getKey() + groupId + "_" + userId);
            groupMember = null;
        }

        if(groupMember == null){
            // 缓存缺失时回源数据库，避免 Redis 被清空后老成员被误判为“未加入群聊”
            groupMember = getActiveMemberFromDb(groupId, userId);
            if(groupMember == null){
                throw new BusinessException("用户未加入群聊");
            }
            redisClient.set(RedisEnum.GROUP_MEMBER.getKey() + groupId + "_" + userId, JSONObject.toJSONString(groupMember));
        }

        return groupMember.getRole();
    }

    /**
     * 从数据库读取状态正常的群成员记录（缓存缺失时的回源兜底）
     * @author doubao
     * @date 2026/11/6
     * @param groupId 群组id
     * @param userId 用户id
     * @return GroupMember
     */
    private GroupMember getActiveMemberFromDb(String groupId, String userId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getStatus, 1)
                .eq(GroupMember::getDelFlag, 0)
                .orderByDesc(GroupMember::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 根据群id获取群成员列表
     * @author wangjinfei
     * @date 2025/8/17 17:47
     * @param groupId
     * @return Set<String>
     */
    @Override
    public Set<String> getGroupMembers(String groupId) {
        String memberKey = RedisEnum.GROUP_MEMBER_ID.getKey() + groupId;
        // 获取群组成员
        Set<String> groupMemberIds = redisClient.zsReverseRange(memberKey, 0, -1, String.class);
        if (groupMemberIds == null) {
            groupMemberIds = new java.util.HashSet<>();
        }
        // 缓存与库双向对账：既补齐「只写库没写缓存」的成员，也清掉已经退群/被移除但还留在 zset 里的幽灵成员。
        // 否则群成员名单、@选人、已读人数都会把已移除的人继续算进来。
        List<GroupMember> dbMembers = this.list(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getDelFlag, 0));
        Set<String> activeMemberIds = new java.util.HashSet<>();
        long score = System.currentTimeMillis();
        if (CollectionUtils.isNotEmpty(dbMembers)) {
            for (GroupMember member : dbMembers) {
                String userId = member.getUserId();
                if (StringUtils.isEmpty(userId)) {
                    continue;
                }
                // status=0 表示已退出，不算有效成员
                if (member.getStatus() != null && member.getStatus() == 0) {
                    continue;
                }
                activeMemberIds.add(userId);
                if (!groupMemberIds.contains(userId)) {
                    groupMemberIds.add(userId);
                    redisClient.zsadd(memberKey, userId, score);
                }
            }
        }
        // 清掉缓存里已不在有效成员集合中的旧成员
        java.util.Iterator<String> iterator = groupMemberIds.iterator();
        while (iterator.hasNext()) {
            String cachedUserId = iterator.next();
            if (!activeMemberIds.contains(cachedUserId)) {
                iterator.remove();
                redisClient.zsrem(memberKey, cachedUserId);
            }
        }
        return groupMemberIds;
    }

    @Override
    public PagePlus<GroupMember> pageByGroupId(String groupId, String keyword, Integer pageNum, Integer pageSize) {
        Page<GroupMember> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GroupMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupMember::getGroupId, groupId)
            .eq(GroupMember::getStatus, 1)
            .eq(GroupMember::getDelFlag, 0);

        if (StringUtils.isNotEmpty(keyword)) {
            List<String> userIds = new ArrayList<>();
            if (StringUtils.isNumeric(keyword)) {
                userIds.add(keyword);
            }

            List<UserSearchVO> users = userService.getListByNameForAdmin(keyword);
            if (CollectionUtils.isNotEmpty(users)) {
                userIds.addAll(users.stream().map(UserSearchVO::getId).collect(Collectors.toList()));
            }
            userIds = userIds.stream().distinct().collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(userIds)) {
                List<String> finalUserIds = userIds;
                queryWrapper.and(wrapper -> wrapper.like(GroupMember::getNickname, keyword)
                    .or()
                    .in(GroupMember::getUserId, finalUserIds));
            } else {
                queryWrapper.like(GroupMember::getNickname, keyword);
            }
        }

        queryWrapper.orderByDesc(GroupMember::getCreateTime);
        Page<GroupMember> memberPage = baseMapper.selectPage(page, queryWrapper);
        return PagePlus.fromPage(memberPage);
    }

    /**
     * 修改群成员角色（设/取消管理员）
     * 角色缓存是权限判断的主路径，改完必须失效，否则后续仍是旧角色
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param userId 目标用户id
     * @param role 角色（1：群主；2：群管理；3：普通成员）
     * @return Boolean 是否修改成功
     */
    @Override
    public Boolean updateRole(String groupId, String userId, Integer role) {
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(userId) || role == null) {
            throw new BusinessException("群ID、用户ID与角色不能为空");
        }

        boolean updated = update(new LambdaUpdateWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .set(GroupMember::getRole, role));

        redisClient.del(RedisEnum.GROUP_MEMBER.getKey() + groupId + "_" + userId);
        return updated;
    }

    /**
     * 更新成员单人禁言截止时间（null 表示解除）
     */
    @Override
    public Boolean updateMuteUntil(String groupId, String userId, LocalDateTime muteUntil) {
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(userId)) {
            throw new BusinessException("群ID或用户ID不能为空");
        }
        boolean updated = update(new LambdaUpdateWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getStatus, 1)
                .set(GroupMember::getMuteUntil, muteUntil));
        redisClient.del(RedisEnum.GROUP_MEMBER.getKey() + groupId + "_" + userId);
        return updated;
    }

    /**
     * 查询成员单人禁言截止时间
     */
    @Override
    public LocalDateTime getMuteUntil(String groupId, String userId) {
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(userId)) {
            return null;
        }
        GroupMember member = getOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getStatus, 1)
                .last("limit 1"));
        return member == null ? null : member.getMuteUntil();
    }

    /**
     * 移除单个群成员（退出群聊时调用）
     * 注意：缓存是群详情/角色的读取主路径，删除 DB 后必须同步清理成员缓存与成员 zset，
     * 否则 getGroupRole / getJoinTime 仍会返回退出前的状态。
     * @author doubao
     * @date 2026/11/6
     * @param groupId 群组id
     * @param userId 用户id
     * @return Boolean 是否移除成功
     */
    @Override
    public Boolean removeGroupMember(String groupId, String userId) {
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(userId)) {
            throw new BusinessException("群ID或用户ID不能为空");
        }

        int deleted = baseMapper.delete(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));

        // 清理缓存
        redisClient.del(RedisEnum.GROUP_MEMBER.getKey() + groupId + "_" + userId);
        redisClient.zsrem(RedisEnum.GROUP_MEMBER_ID.getKey() + groupId, userId);

        return deleted > 0;
    }

    /**
     * 移除群内全部成员（解散群聊时调用）
     * @author doubao
     * @date 2026/11/6
     * @param groupId 群组id
     * @return Boolean 是否移除成功
     */
    @Override
    public Boolean removeAllGroupMember(String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            throw new BusinessException("群组id不能为空");
        }

        List<GroupMember> members = getListByGroupId(groupId);
        baseMapper.delete(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId));

        for (GroupMember member : members) {
            redisClient.del(RedisEnum.GROUP_MEMBER.getKey() + groupId + "_" + member.getUserId());
        }
        redisClient.del(RedisEnum.GROUP_MEMBER_ID.getKey() + groupId);

        return true;
    }
}
