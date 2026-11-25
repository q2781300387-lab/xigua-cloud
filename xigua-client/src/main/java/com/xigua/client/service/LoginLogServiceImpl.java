package com.xigua.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.api.service.LoginLogService;
import com.xigua.client.mapper.LoginLogMapper;
import com.xigua.common.core.util.Ip2RegionUtil;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.bo.LoginLogFailBO;
import com.xigua.domain.bo.LoginLogSuccessBO;
import com.xigua.domain.bo.LogoutBO;
import com.xigua.domain.entity.LoginLog;
import com.xigua.domain.enums.LoginStatus;
import com.xigua.domain.enums.LoginType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ClassName LoginLogServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/15 10:22
 */
@Slf4j
@Service
@DubboService
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {
    @Autowired
    private Sequence sequence;

    @Override
    public void recordLoginSuccess(LoginLogSuccessBO successBO) {
        LoginLog log = new LoginLog();
        log.setId(sequence.nextNo());
        log.setUserId(successBO.getUserId());
        log.setLoginType(successBO.getLoginType());
        log.setLoginIp(successBO.getLoginIp());
        log.setLoginRegion(Ip2RegionUtil.search(successBO.getLoginIp()));
        log.setUserAgent(successBO.getUserAgent());
        log.setPlatform(successBO.getPlatform());
        log.setLoginStatus(LoginStatus.SUCCESS.getType());
        log.setLoginMsg("登录成功");
        log.setCreateBy(successBO.getUserId());
        baseMapper.insert(log);
    }

    @Override
    public void recordLoginFail(LoginLogFailBO failBO) {
        LoginLog log = new LoginLog();
        log.setId(sequence.nextNo());
        log.setUserId(failBO.getUserId());
        log.setLoginType(failBO.getLoginType());
        log.setLoginIp(failBO.getLoginIp());
        log.setLoginRegion(Ip2RegionUtil.search(failBO.getLoginIp()));
        log.setUserAgent(failBO.getUserAgent());
        log.setPlatform(failBO.getPlatform());
        log.setLoginStatus(LoginStatus.FAIL.getType());
        log.setLoginMsg(failBO.getFailMsg());
        baseMapper.insert(log);
    }

    @Override
    public void recordLogout(LogoutBO logoutBO) {
        LoginLog log = new LoginLog();
        log.setId(sequence.nextNo());
        log.setUserId(logoutBO.getUserId());
        log.setLoginType(LoginType.LOG_OUT.getType());
        log.setLoginIp(logoutBO.getLoginIp());
        log.setLoginRegion(Ip2RegionUtil.search(logoutBO.getLoginIp()));
        log.setUserAgent(logoutBO.getUserAgent());
        log.setPlatform(logoutBO.getPlatform());
        log.setLoginStatus(LoginStatus.SUCCESS.getType());
        log.setLoginMsg("登出成功");
        baseMapper.insert(log);
    }

    @Override
    public com.xigua.common.mybatis.base.PagePlus<LoginLog> page(String username, Integer loginType, Integer loginStatus, String startTime, String endTime, Integer pageNum, Integer pageSize) {
        Page<LoginLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LoginLog> queryWrapper = new LambdaQueryWrapper<>();

        // 用户名筛选（需要先查询用户ID）
        if(StringUtils.isNotEmpty(username) && !"null".equals(username)){
            queryWrapper.like(LoginLog::getUserId, username);
        }

        // 操作类型筛选
        if(loginType != null){
            queryWrapper.eq(LoginLog::getLoginType, loginType);
        }

        // 登录状态筛选
        if(loginStatus != null){
            queryWrapper.eq(LoginLog::getLoginStatus, loginStatus);
        }

        // 时间范围筛选（格式：yyyy-MM-dd，自动补充时间范围）
        if(StringUtils.isNotEmpty(startTime) && !"null".equals(startTime)){
            String start = startTime.length() == 10 ? startTime + " 00:00:00" : startTime;
            queryWrapper.ge(LoginLog::getCreateTime, start);
        }
        if(StringUtils.isNotEmpty(endTime) && !"null".equals(endTime)){
            String end = endTime.length() == 10 ? endTime + " 23:59:59" : endTime;
            queryWrapper.le(LoginLog::getCreateTime, end);
        }

        queryWrapper.orderByDesc(LoginLog::getCreateTime);
        Page<LoginLog> loginLogPage = baseMapper.selectPage(page, queryWrapper);
        return com.xigua.common.mybatis.base.PagePlus.fromPage(loginLogPage);
    }

    @Override
    public java.time.LocalDateTime getLastLoginTime(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return null;
        }
        // 查询用户最后一次成功登录的记录
        LoginLog lastLogin = baseMapper.selectOne(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUserId, userId)
                .eq(LoginLog::getLoginStatus, 1) // 登录成功
                .orderByDesc(LoginLog::getCreateTime)
                .last("LIMIT 1"));
        return lastLogin != null ? lastLogin.getCreateTime() : null;
    }
}
