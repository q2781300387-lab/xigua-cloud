package com.xigua.admin.service;

import com.xigua.api.service.GroupMemberService;
import com.xigua.api.service.GroupService;
import com.xigua.api.service.UserService;
import com.xigua.api.service.admin.AdminGroupService;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.common.mybatis.base.PagePlus;
import com.xigua.domain.dto.QueryGroupPageDTO;
import com.xigua.domain.dto.QueryGroupMemberPageDTO;
import com.xigua.domain.entity.Group;
import com.xigua.domain.entity.GroupMember;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.GroupRole;
import com.xigua.domain.util.BasePage;
import com.xigua.domain.vo.admin.AdminGroupDetailVO;
import com.xigua.domain.vo.admin.AdminGroupMemberPageVO;
import com.xigua.domain.vo.admin.AdminGroupPageVO;
import com.xigua.domain.vo.admin.AdminGroupStatisticsVO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName AdminGroupServiceImpl
 * @Description 管理端群组服务实现
 * @Author wangjinfei
 * @Date 2026/3/7
 */
@Service
@DubboService
public class AdminGroupServiceImpl implements AdminGroupService {

    @DubboReference
    private GroupService groupService;

    @DubboReference
    private UserService userService;

    @DubboReference
    private GroupMemberService groupMemberService;

    @DubboReference
    private com.xigua.api.service.ChatMessageService chatMessageService;

    @Autowired
    private RedisClient redisClient;

    private static final String GROUP_STATISTICS_CACHE_KEY = "admin:group:statistics";
    private static final int CACHE_TTL_SECONDS = 300;

    @Override
    public BasePageVO<AdminGroupPageVO> page(QueryGroupPageDTO dto) {
        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        List<AdminGroupPageVO> voList = new ArrayList<>();

        // 分页查询群组
        PagePlus<Group> groupPage = groupService.page(
                dto.getGroupName(),
                dto.getOwnerName(),
                dto.getCreateTime(),
                pageNum,
                pageSize
        );

        // 组装VO
        for (Group group : groupPage.getRecords()) {
            AdminGroupPageVO vo = new AdminGroupPageVO();
            BeanUtils.copyProperties(group, vo);

            // 设置状态（delFlag转为status）
            vo.setStatus(group.getDelFlag());
            vo.setMuteAll(group.getMuteAll());

            // 设置成员人数
            vo.setMemberCount(group.getCurrentMember());

            // 查询群主用户名
            if (group.getOwnerId() != null) {
                User owner = userService.getById(group.getOwnerId());
                if (owner != null) {
                    vo.setOwnerName(owner.getUsername());
                }
            }

            voList.add(vo);
        }

        return BasePage.getResult(groupPage, voList);
    }

    @Override
    public AdminGroupDetailVO getDetail(String groupId) {
        if (!StringUtils.hasText(groupId)) {
            return null;
        }

        Group group = groupService.getById(groupId);
        if (group == null) {
            return null;
        }

        AdminGroupDetailVO vo = new AdminGroupDetailVO();
        BeanUtils.copyProperties(group, vo);
        vo.setStatus(group.getDelFlag());
        vo.setMuteAll(group.getMuteAll());
        vo.setMemberCount(group.getCurrentMember());

        if (group.getOwnerId() != null) {
            User owner = userService.getById(group.getOwnerId());
            if (owner != null) {
                vo.setOwnerName(owner.getUsername());
            }
        }

        return vo;
    }

    @Override
    public AdminGroupStatisticsVO getStatistics(String groupId) {
        if (!StringUtils.hasText(groupId)) {
            return null;
        }

        String cacheKey = GROUP_STATISTICS_CACHE_KEY + ":" + groupId;
        String cached = redisClient.get(cacheKey);
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(cached)) {
            try {
                AdminGroupStatisticsVO cachedVO = com.alibaba.fastjson2.JSONObject.parseObject(
                    cached, AdminGroupStatisticsVO.class
                );
                if (cachedVO != null) {
                    return cachedVO;
                }
            } catch (Exception ignored) {
            }
        }

        Group group = groupService.getById(groupId);
        if (group == null) {
            return null;
        }

        AdminGroupStatisticsVO vo = new AdminGroupStatisticsVO();
        Long memberTotal = group.getCurrentMember() == null ? 0L : group.getCurrentMember().longValue();
        vo.setMemberTotal(memberTotal);
        vo.setMemberGrowthRate(0.0);

        Long yesterdayActiveCount = 0L;
        vo.setYesterdayActiveCount(yesterdayActiveCount);
        vo.setYesterdayActiveGrowthRate(0.0);

        Long totalMessages = chatMessageService.countGroupMessages(groupId);
        vo.setTotalMessageCount(totalMessages);
        vo.setTotalMessageGrowthRate(0.0);

        try {
            redisClient.set(cacheKey,
                com.alibaba.fastjson2.JSONObject.toJSONString(vo), CACHE_TTL_SECONDS);
        } catch (Exception ignored) {
        }

        return vo;
    }

    @Override
    public BasePageVO<AdminGroupMemberPageVO> pageMembers(String groupId, QueryGroupMemberPageDTO dto) {
        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        PagePlus<GroupMember> memberPage = groupMemberService.pageByGroupId(groupId, dto.getMemberName(), pageNum, pageSize);
        List<GroupMember> members = memberPage.getRecords();
        if (members == null || members.isEmpty()) {
            return BasePage.emptyResult(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize));
        }

        List<String> userIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
        List<User> users = userService.getListByIds(userIds);
        Map<String, User> userMap = users.stream().collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
        Map<String, LocalDateTime> lastMessageTimeMap = chatMessageService.getGroupMemberLastMessageTimes(groupId, userIds);
        if (lastMessageTimeMap == null) {
            lastMessageTimeMap = java.util.Map.of();
        }

        List<AdminGroupMemberPageVO> voList = new ArrayList<>();
        for (GroupMember member : members) {
            AdminGroupMemberPageVO vo = new AdminGroupMemberPageVO();
            vo.setUserId(member.getUserId());
            vo.setNickname(member.getNickname());
            vo.setRole(member.getRole());
            GroupRole groupRole = GroupRole.getByType(member.getRole());
            if (groupRole != null) {
                vo.setRoleName(groupRole.getDesc());
            }
            vo.setJoinTime(member.getCreateTime());

            User user = userMap.get(member.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setAvatar(user.getAvatar());
            }

            vo.setLastMessageTime(lastMessageTimeMap.get(member.getUserId()));

            voList.add(vo);
        }

        return BasePage.getResult(memberPage, voList);
    }

    @Override
    public Boolean setMuteAll(String groupId, Integer muteAll) {
        Boolean result = groupService.adminSetMuteAll(groupId, muteAll);
        clearStatisticsCache(groupId);
        return result;
    }

    @Override
    public Boolean disbandGroup(String groupId) {
        Boolean result = groupService.adminDisbandGroup(groupId);
        clearStatisticsCache(groupId);
        return result;
    }

    @Override
    public Boolean removeMember(String groupId, String userId) {
        Boolean result = groupService.adminRemoveMember(groupId, userId);
        clearStatisticsCache(groupId);
        return result;
    }

    private void clearStatisticsCache(String groupId) {
        try {
            redisClient.del(GROUP_STATISTICS_CACHE_KEY + ":" + groupId);
        } catch (Exception ignored) {
        }
    }

    private Double calculateGrowthRate(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return current != null && current > 0 ? 100.0 : 0.0;
        }
        return (double) (current - previous) / previous * 100;
    }

    private Long getYesterdayStartMillis() {
        LocalDateTime yesterdayStart = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MIN);
        return yesterdayStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

}
