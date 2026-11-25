package com.xigua.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigua.api.service.ContactService;
import com.xigua.api.service.UserService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.PasswordUtil;
import com.xigua.common.core.util.TokenUtil;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.client.mapper.LoginLogMapper;
import com.xigua.client.mapper.UserBlockMapper;
import com.xigua.client.mapper.UserDataExportMapper;
import com.xigua.client.mapper.UserPrivacySettingMapper;
import com.xigua.client.mapper.UserReportMapper;
import com.xigua.domain.entity.LoginLog;
import com.xigua.domain.entity.User;
import com.xigua.domain.entity.UserBlock;
import com.xigua.domain.entity.UserPrivacySetting;
import com.xigua.domain.entity.UserReport;
import com.xigua.domain.vo.LoginDeviceVO;
import com.xigua.domain.vo.PrivacySettingVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PrivacyServiceImpl {

    @Autowired
    private UserPrivacySettingMapper settingMapper;
    @Autowired
    private UserBlockMapper blockMapper;
    @Autowired
    private UserReportMapper reportMapper;
    @Autowired
    private LoginLogMapper loginLogMapper;
    @Autowired
    private UserDataExportMapper userDataExportMapper;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private Sequence sequence;
    @DubboReference
    private ContactService contactService;

    @DubboReference
    private UserService userService;

    private String uid() {
        return UserContext.get().getUserId();
    }

    private UserPrivacySetting row(String userId) {
        return settingMapper.selectOne(new LambdaQueryWrapper<UserPrivacySetting>()
                .eq(UserPrivacySetting::getUserId, userId).last("limit 1"));
    }

    public PrivacySettingVO getSettings() {
        PrivacySettingVO vo = new PrivacySettingVO();
        UserPrivacySetting row = row(uid());
        if (row != null) {
            BeanUtils.copyProperties(row, vo);
        }
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public PrivacySettingVO saveSettings(PrivacySettingVO input) {
        String userId = uid();
        UserPrivacySetting row = row(userId);
        boolean created = row == null;
        if (created) {
            row = new UserPrivacySetting();
            row.setId(sequence.nextNo());
            row.setUserId(userId);
        }
        row.setNotificationPreview(flag(input.getNotificationPreview()));
        row.setNotificationSound(flag(input.getNotificationSound()));
        row.setShowOnlineStatus(flag(input.getShowOnlineStatus()));
        row.setShowReadReceipt(flag(input.getShowReadReceipt()));
        row.setAllowPhoneSearch(flag(input.getAllowPhoneSearch()));
        row.setFriendVerify(flag(input.getFriendVerify()));
        if (created) {
            settingMapper.insert(row);
        } else {
            settingMapper.updateById(row);
        }
        return getSettings();
    }

    private Integer flag(Integer v) {
        return v != null && v == 0 ? 0 : 1;
    }

    public List<Map<String, Object>> blocks() {
        List<UserBlock> rows = blockMapper.selectList(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getUserId, uid()).orderByDesc(UserBlock::getCreateTime));
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserBlock row : rows) {
            User user = userService.getById(row.getBlockedUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.getBlockedUserId());
            item.put("name", user == null ? "未知用户" : user.getUsername());
            item.put("avatar", user == null ? null : user.getAvatar());
            item.put("blockedAt", row.getCreateTime());
            list.add(item);
        }
        return list;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean block(String targetId) {
        if (StringUtils.isBlank(targetId)) throw new BusinessException("请选择要屏蔽的用户");
        if (targetId.equals(uid())) throw new BusinessException("不能屏蔽自己");
        if (userService.getById(targetId) == null) throw new BusinessException("用户不存在");
        long exists = blockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getUserId, uid()).eq(UserBlock::getBlockedUserId, targetId));
        if (exists == 0) {
            UserBlock row = new UserBlock();
            row.setId(sequence.nextNo());
            row.setUserId(uid());
            row.setBlockedUserId(targetId);
            blockMapper.insert(row);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean unblock(String targetId) {
        blockMapper.delete(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getUserId, uid()).eq(UserBlock::getBlockedUserId, targetId));
        return true;
    }

    public Boolean isBlocked(String targetId) {
        return blockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getUserId, uid()).eq(UserBlock::getBlockedUserId, targetId)) > 0;
    }

    public List<LoginDeviceVO> devices(HttpServletRequest request) {
        String userId = uid();
        List<LoginLog> logs = loginLogMapper.selectList(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUserId, userId).eq(LoginLog::getLoginStatus, 1)
                .orderByDesc(LoginLog::getCreateTime).last("limit 30"));
        String currentUa = request.getHeader("User-Agent");
        String currentIp = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(currentIp)) currentIp = request.getRemoteAddr();
        Map<String, LoginDeviceVO> unique = new LinkedHashMap<>();
        for (LoginLog logRow : logs) {
            String key = (logRow.getUserAgent() == null ? "" : logRow.getUserAgent()) + "|" + logRow.getLoginIp();
            if (unique.containsKey(key)) continue;
            LoginDeviceVO vo = new LoginDeviceVO();
            vo.setId(String.valueOf(logRow.getId()));
            vo.setUserAgent(logRow.getUserAgent());
            vo.setIp(logRow.getLoginIp());
            vo.setRegion(logRow.getLoginRegion());
            vo.setLastLoginTime(logRow.getCreateTime());
            vo.setPlatform(platformName(logRow.getPlatform()));
            boolean current = StringUtils.equals(currentUa, logRow.getUserAgent())
                    && (StringUtils.isBlank(logRow.getLoginIp()) || currentIp == null || currentIp.contains(logRow.getLoginIp()));
            vo.setCurrent(current);
            unique.put(key, vo);
            if (unique.size() >= 10) break;
        }
        List<LoginDeviceVO> list = new ArrayList<>(unique.values());
        if (!list.isEmpty() && list.stream().noneMatch(LoginDeviceVO::getCurrent)) {
            list.get(0).setCurrent(true);
        }
        return list;
    }

    private String platformName(Integer platform) {
        if (platform == null) return "Web";
        return platform == 2 ? "App" : platform == 3 ? "小程序" : "Web";
    }

    public int kickOtherDevices(String currentToken) {
        if (StringUtils.isBlank(currentToken)) throw new BusinessException("登录状态异常，请重新登录");
        String userId = uid();
        String currentKey = TokenUtil.deviceTokenKey(userId, currentToken);
        Set<String> keys = redisClient.getKeysByKey("TOKEN:" + userId + ":device:*");
        int count = 0;
        for (String key : keys) {
            if (!key.equals(currentKey)) {
                redisClient.del(key);
                count++;
            }
        }
        return count;
    }

    public Boolean report(String targetId, String targetType, String reason, String detail) {
        if (StringUtils.isBlank(targetId)) throw new BusinessException("请选择举报对象");
        if (StringUtils.isBlank(reason)) throw new BusinessException("请选择举报原因");
        UserReport row = new UserReport();
        row.setId(sequence.nextNo());
        row.setReporterId(uid());
        row.setTargetId(targetId);
        row.setTargetType(StringUtils.defaultIfBlank(targetType, "user"));
        row.setReason(reason);
        row.setDetail(StringUtils.abbreviate(detail == null ? "" : detail, 500));
        row.setStatus(0);
        reportMapper.insert(row);
        return true;
    }

    public Map<String, Object> exportData() {
        String userId = uid();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportTime", LocalDateTime.now());
        User me = userService.getById(userId);
        if (me != null) {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("id", me.getId());
            profile.put("username", me.getUsername());
            profile.put("email", me.getEmail());
            profile.put("phone", me.getPhone());
            profile.put("avatar", me.getAvatar());
            profile.put("signature", me.getSignature());
            profile.put("createTime", me.getCreateTime());
            data.put("profile", profile);
        }
        data.put("privacy", getSettings());
        data.put("contacts", contactService.getFriendList());
        data.put("groups", contactService.getGroupList());
        data.put("blocks", blocks());
        data.put("messages", userDataExportMapper.selectMessages(userId, 500));
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deactivate(String password, String confirmText) {
        if (!"注销账号".equals(StringUtils.trim(confirmText))) {
            throw new BusinessException("请输入“注销账号”确认");
        }
        if (StringUtils.isBlank(password)) throw new BusinessException("请输入密码");
        User user = userService.getById(uid());
        if (user == null) throw new BusinessException("用户不存在");
        if (!PasswordUtil.matches(password, user.getPassword())) throw new BusinessException("密码错误");
        user.setStatus(3);
        userService.updateById(user);
        String userId = user.getId();
        for (String key : redisClient.getKeysByKey("TOKEN:" + userId + ":device:*")) {
            redisClient.del(key);
        }
        redisClient.del("TOKEN:" + userId);
        redisClient.del("USER:" + userId);
        log.info("用户自助注销 userId={}", userId);
        return true;
    }

    public Map<String, Object> reportStats() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pending", reportMapper.selectCount(new LambdaQueryWrapper<UserReport>()
                .eq(UserReport::getStatus, 0)));
        return map;
    }
}
