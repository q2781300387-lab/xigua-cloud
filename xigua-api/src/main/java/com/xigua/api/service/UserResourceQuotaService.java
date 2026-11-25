package com.xigua.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.domain.entity.UserResourceQuota;

/**
 * @ClassName UserResourceQuotaService
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/27 11:15
 */
public interface UserResourceQuotaService extends IService<UserResourceQuota> {
    /**
     * 根据用户id获取
     * @author wangjinfei
     * @date 2026/1/27 13:25
     * @param userId
     * @return UserResourceQuota
    */
    UserResourceQuota getByUserId(String userId);

    /**
     * 添加默认容量
     * @author wangjinfei
     * @date 2026/1/27 19:29
     * @param userId
    */
    void insertDefaultQuota(String userId);
}
