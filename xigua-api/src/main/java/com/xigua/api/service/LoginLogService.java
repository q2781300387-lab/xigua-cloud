package com.xigua.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.domain.bo.LoginLogFailBO;
import com.xigua.domain.bo.LoginLogSuccessBO;
import com.xigua.domain.bo.LogoutBO;
import com.xigua.domain.entity.LoginLog;

/**
 * @ClassName LoginLogService
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/15 10:00
 */
public interface LoginLogService extends IService<LoginLog> {
    /**
     * 记录登录成功
     */
    void recordLoginSuccess(LoginLogSuccessBO successBO);

    /**
     * 记录登录失败
     */
    void recordLoginFail(LoginLogFailBO failBO);

    /**
     * 记录登出（主动登出）
     */
    void recordLogout(LogoutBO logoutBO);

    /**
     * 分页查询登录日志
     * @author wangjinfei
     * @date 2026/3/8
     * @param username 用户名
     * @param loginType 操作类型
     * @param loginStatus 登录状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return PagePlus<LoginLog>
     */
    com.xigua.common.mybatis.base.PagePlus<LoginLog> page(String username, Integer loginType, Integer loginStatus, String startTime, String endTime, Integer pageNum, Integer pageSize);

    /**
     * 获取用户最后登录时间
     * @author wangjinfei
     * @date 2026/3/10
     * @param userId 用户ID
     * @return 最后登录时间
     */
    java.time.LocalDateTime getLastLoginTime(String userId);
}
