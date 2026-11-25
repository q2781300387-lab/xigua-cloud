package com.xigua.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.api.service.SyncRegionService;
import com.xigua.client.mapper.LoginLogMapper;
import com.xigua.common.core.util.Ip2RegionUtil;
import com.xigua.domain.entity.LoginLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 同步区域服务实现
 * 用于补充历史登录日志的区域信息
 *
 * @ClassName SyncRegionServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/3/9
 */
@Slf4j
@Service
@DubboService
public class SyncRegionServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements SyncRegionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncLoginRegion() {
        // 查询所有 login_region 为空但 login_ip 不为空且不为 unknown 的记录
        LambdaQueryWrapper<LoginLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                        .isNull(LoginLog::getLoginRegion)
                        .or()
                        .eq(LoginLog::getLoginRegion, ""))
                .isNotNull(LoginLog::getLoginIp)
                .ne(LoginLog::getLoginIp, "")
                .ne(LoginLog::getLoginIp, "unknown");

        List<LoginLog> loginLogs = baseMapper.selectList(queryWrapper);
        if (loginLogs == null || loginLogs.isEmpty()) {
            log.info("No login logs need to sync region");
            return 0;
        }

        int successCount = 0;
        int failCount = 0;

        for (LoginLog log : loginLogs) {
            boolean result = syncLoginRegionById(log.getId());
            if (result) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("Sync login region completed, success: {}, fail: {}", successCount, failCount);
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncLoginRegionById(String id) {
        LoginLog loginLog = baseMapper.selectById(id);
        if (loginLog == null) {
            log.warn("Login log not found, id: {}", id);
            return false;
        }

        String loginIp = loginLog.getLoginIp();
        if (StringUtils.isEmpty(loginIp) || "unknown".equals(loginIp)) {
            log.warn("Login ip is empty or unknown, id: {}, ip: {}", id, loginIp);
            return false;
        }

        // 如果已经有区域信息，不再重复同步
        if (StringUtils.isNotEmpty(loginLog.getLoginRegion())) {
            log.info("Login region already exists, id: {}, region: {}", id, loginLog.getLoginRegion());
            return true;
        }

        try {
            // 根据 IP 查询区域信息
            String region = Ip2RegionUtil.search(loginIp);

            // 如果查询结果为 unknown，也记录并返回成功
            if ("unknown".equals(region)) {
                LoginLog update = new LoginLog();
                update.setId(id);
                update.setLoginRegion(region);
                baseMapper.updateById(update);
                log.info("Sync login region success with unknown, id: {}, ip: {}", id, loginIp);
                return true;
            }

            // 更新登录区域
            LoginLog update = new LoginLog();
            update.setId(id);
            update.setLoginRegion(region);

            int rows = baseMapper.updateById(update);
            if (rows > 0) {
                log.info("Sync login region success, id: {}, ip: {}, region: {}", id, loginIp, region);
                return true;
            } else {
                log.warn("Sync login region failed, id: {}, ip: {}", id, loginIp);
                return false;
            }
        } catch (Exception e) {
            log.error("Sync login region error, id: {}, ip: {}", id, loginIp, e);
            return false;
        }
    }
}
