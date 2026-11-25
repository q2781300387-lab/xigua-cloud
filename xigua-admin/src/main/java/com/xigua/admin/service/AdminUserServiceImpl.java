package com.xigua.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xigua.api.service.ChatMessageService;
import com.xigua.api.service.LoginLogService;
import com.xigua.api.service.StatsService;
import com.xigua.api.service.UserService;
import com.xigua.api.service.admin.AdminUserService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.StringUtil;
import com.xigua.domain.dto.QueryUserPageDTO;
import com.xigua.domain.dto.UpdateUserDTO;
import com.xigua.domain.entity.User;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.common.mybatis.base.PagePlus;
import com.xigua.domain.util.BasePage;
import com.xigua.domain.vo.admin.ActivityStatsVO;
import com.xigua.domain.vo.admin.AdminUserDetailVO;
import com.xigua.domain.vo.admin.AdminUserPage;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName AdminUserServiceImpl
 * @Description 管理端用户服务实现
 * @Author wangjinfei
 * @Date 2026/2/1 17:06
 */
@Service
@DubboService
public class AdminUserServiceImpl implements AdminUserService {
    @DubboReference
    private UserService userService;
    @DubboReference
    private ChatMessageService chatMessageService;
    @DubboReference
    private StatsService statsService;
    @DubboReference
    private LoginLogService loginLogService;

    @Override
    public BasePageVO<AdminUserPage> page(QueryUserPageDTO dto) {
        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        Page<User> page = new Page<>(pageNum, pageSize);
        List<AdminUserPage> pages = new ArrayList<>();

        PagePlus<User> userPage = userService.page(dto);

        for (User user : userPage.getRecords()) {
            AdminUserPage adminUserPage = new AdminUserPage();
            BeanUtils.copyProperties(user, adminUserPage);
            String email = adminUserPage.getEmail();
            String maskedEmail = StringUtil.maskEmail(email);
            adminUserPage.setEmail(maskedEmail);
            pages.add(adminUserPage);
        }

        BasePageVO<AdminUserPage> result = BasePage.getResult(userPage, pages);
        return result;
    }

    @Override
    public AdminUserDetailVO getDetail(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return null;
        }

        User user = userService.getById(userId);
        if (user == null) {
            return null;
        }

        AdminUserDetailVO vo = new AdminUserDetailVO();
        BeanUtils.copyProperties(user, vo);

        // 邮箱脱敏
        String email = user.getEmail();
        if (StringUtils.isNotEmpty(email)) {
            vo.setEmail(StringUtil.maskEmail(email));
        }

        // 获取最后登录时间
        java.time.LocalDateTime lastTime = loginLogService.getLastLoginTime(userId);
        vo.setLastLoginTime(lastTime);

        return vo;
    }

    @Override
    public ActivityStatsVO getActivityStats(String userId) {
        ActivityStatsVO vo = new ActivityStatsVO();

        // 获取用户发送的消息总数
        Long totalMessages = chatMessageService.getTotalMessagesByUser(userId);
        vo.setSentMessages(totalMessages != null ? totalMessages : 0L);

        // 计算消息增长趋势（今日 vs 昨日）
        Long todayMessages = chatMessageService.countTodayMessagesByUser(userId);
        Long yesterdayMessages = chatMessageService.countYesterdayMessagesByUser(userId);
        if (yesterdayMessages != null && yesterdayMessages > 0) {
            double trend = ((todayMessages - yesterdayMessages) * 100.0) / yesterdayMessages;
            vo.setMessageTrend(Math.round(trend * 10.0) / 10.0); // 保留一位小数
        } else {
            vo.setMessageTrend(0.0);
        }

        // 获取用户加入的群组数
        Long userGroups = statsService.getUserGroupCount(userId);
        vo.setActiveGroups(userGroups != null ? userGroups : 0L);

        // 计算群组增长趋势（今日 vs 昨日）
        Long yesterdayGroups = statsService.getUserYesterdayGroupCount(userId);
        if (yesterdayGroups != null && yesterdayGroups > 0) {
            double trend = ((userGroups - yesterdayGroups) * 100.0) / yesterdayGroups;
            vo.setGroupTrend(Math.round(trend * 10.0) / 10.0); // 保留一位小数
        } else {
            vo.setGroupTrend(0.0);
        }

        // 获取用户好友总数
        Long userFriends = statsService.getUserFriendCount(userId);
        vo.setTotalFriends(userFriends != null ? userFriends : 0L);

        // 好友增长趋势（这里暂时设置为0，后续可根据实际需求计算）
        vo.setFriendTrend(0.0);

        return vo;
    }

    @Override
    public Boolean updateUser(UpdateUserDTO dto) {
        if (dto == null || StringUtils.isEmpty(dto.getId())) {
            throw new BusinessException("用户ID不能为空");
        }

        User existingUser = userService.getById(dto.getId());
        if (existingUser == null) {
            throw new BusinessException("用户不存在");
        }

        String newUsername = dto.getUsername();
        if (StringUtils.isNotEmpty(newUsername) && !newUsername.equals(existingUser.getUsername())) {
            Long usernameCount = userService.getCountByName(newUsername);
            if (usernameCount != null && usernameCount > 0) {
                throw new BusinessException("用户名已存在");
            }
        }

        String newEmail = dto.getEmail();
        if (StringUtils.isNotEmpty(newEmail) && !newEmail.equals(existingUser.getEmail())) {
            Long emailCount = userService.getCountByEmail(newEmail);
            if (emailCount != null && emailCount > 0) {
                throw new BusinessException("邮箱已存在");
            }
        }

        User user = new User();
        BeanUtils.copyProperties(dto, user);
        return userService.updateUserInfoForAdmin(user);
    }

    @Override
    public Boolean banUser(String userId, String reason) {
        return userService.updateModeration(userId, 2, null, normalizeReason(reason));
    }

    @Override
    public Boolean unbanUser(String userId) {
        return userService.updateModeration(userId, 0, null, null);
    }

    @Override
    public Boolean muteUser(String userId, Integer durationHours, String reason) {
        LocalDateTime muteUntil = null;
        if (durationHours != null && durationHours > 0) {
            if (durationHours > 24 * 365 * 10) {
                throw new BusinessException("禁言时长不能超过 10 年");
            }
            muteUntil = LocalDateTime.now().plusHours(durationHours);
        }
        return userService.updateModeration(userId, 1, muteUntil, normalizeReason(reason));
    }

    @Override
    public Boolean unmuteUser(String userId) {
        return userService.updateModeration(userId, 0, null, null);
    }

    private String normalizeReason(String reason) {
        if (StringUtils.isBlank(reason)) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }
}
