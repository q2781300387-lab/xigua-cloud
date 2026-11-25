package com.xigua.center.service;

import com.xigua.api.service.SyncUserResourceQuotaService;
import com.xigua.api.service.UserResourceQuotaService;
import com.xigua.api.service.UserService;
import com.xigua.domain.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName SyncUserResourceQuotaServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/27 19:37
 */
@Slf4j
@Service
@DubboService
public class SyncUserResourceQuotaServiceImpl implements SyncUserResourceQuotaService {
    @DubboReference
    private UserService userService;
    @DubboReference
    private UserResourceQuotaService userResourceQuotaService;


    @Override
    public void syncUser() {
        List<User> users = userService.list();
        if(CollectionUtils.isEmpty(users)){
            // 用一下日志记录
            log.info("无用户");
        }

        for (User user : users) {
            userResourceQuotaService.insertDefaultQuota(user.getId());
            log.info("用户:{}，已添加默认容量", user.getId());
        }
    }
}
