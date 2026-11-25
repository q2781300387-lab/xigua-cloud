package com.xigua.admin.service;

import com.xigua.api.service.LoginLogService;
import com.xigua.api.service.UserService;
import com.xigua.api.service.admin.AdminLoginLogService;

import com.xigua.common.core.util.IpUtil;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.common.mybatis.base.PagePlus;
import com.xigua.domain.dto.QueryLoginLogPageDTO;
import com.xigua.domain.entity.LoginLog;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.LoginStatus;
import com.xigua.domain.enums.LoginType;
import com.xigua.domain.util.BasePage;
import com.xigua.domain.vo.admin.AdminLoginLogPageVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName AdminLoginLogServiceImpl
 * @Description 管理端登录日志服务实现
 * @Author wangjinfei
 * @Date 2026/3/8
 */
@Service
@DubboService
public class AdminLoginLogServiceImpl implements AdminLoginLogService {

    @DubboReference
    private LoginLogService loginLogService;

    @DubboReference
    private UserService userService;

    @Override
    public BasePageVO<AdminLoginLogPageVO> page(QueryLoginLogPageDTO dto) {
        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        List<AdminLoginLogPageVO> voList = new ArrayList<>();

        // 分页查询登录日志
        PagePlus<LoginLog> loginLogPage = loginLogService.page(
                dto.getUsername(),
                dto.getLoginType(),
                dto.getLoginStatus(),
                dto.getStartTime(),
                dto.getEndTime(),
                pageNum,
                pageSize
        );

        // 组装VO
        for (LoginLog log : loginLogPage.getRecords()) {
            AdminLoginLogPageVO vo = new AdminLoginLogPageVO();
            BeanUtils.copyProperties(log, vo);

            // 设置登录时间
            vo.setCreateTime(log.getCreateTime());

            // 设置用户名和用户ID
            // 登录成功时，userId 是真实用户ID，需要查询用户名
            // 登录失败时，userId 存的是用户名，直接赋值
            if (log.getLoginStatus() != null && log.getLoginStatus() == 1) {
                // 登录成功
                vo.setUserId(log.getUserId());
                if (log.getUserId() != null) {
                    User user = userService.getById(log.getUserId());
                    if (user != null) {
                        vo.setUsername(user.getUsername());
                    }
                }
            } else {
                // 登录失败，userId 字段存的是用户名
                vo.setUsername(log.getUserId());
                vo.setUserId(null);
            }

            // 设置操作描述
            if (log.getLoginType() != null) {
                LoginType typeEnum = LoginType.getChatType(log.getLoginType());
                vo.setLoginTypeDesc(typeEnum != null ? typeEnum.getDesc() : "");
            }

            // 设置状态描述（登录失败时拼接失败原因）
            if (log.getLoginStatus() != null) {
                LoginStatus statusEnum = LoginStatus.getChatType(log.getLoginStatus());
                String statusDesc = statusEnum != null ? statusEnum.getDesc() : "";
                // 如果是登录失败，拼接失败原因
                if (log.getLoginStatus() == 0 && StringUtils.isNotEmpty(log.getLoginMsg())) {
                    statusDesc = "(" + log.getLoginMsg() + ")";
                }
                vo.setLoginStatusDesc(statusDesc);
            }

            // IP脱敏（保留前两段，后两段用***代替）
            String loginIp = log.getLoginIp();
            if (loginIp != null) {
                vo.setLoginIp(IpUtil.maskIp(loginIp));
            }

            // 登录区域脱敏处理
            String loginRegion = log.getLoginRegion();
            if (StringUtils.isNotEmpty(loginRegion)) {
                vo.setLoginRegion(IpUtil.maskRegion(loginRegion));
            }else {
                vo.setLoginRegion("unknown");
            }

            // UA直接返回，前端解析
            vo.setUserAgent(log.getUserAgent());

            voList.add(vo);
        }

        return BasePage.getResult(loginLogPage, voList);
    }
}
