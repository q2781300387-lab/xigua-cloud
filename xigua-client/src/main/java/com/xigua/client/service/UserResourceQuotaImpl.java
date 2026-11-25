package com.xigua.client.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.api.service.UserResourceQuotaService;
import com.xigua.client.mapper.UserResourceQuotaMapper;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.entity.UserResourceQuota;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ClassName UserResourceQuotaImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/27 11:18
 */
@Service
@DubboService
public class UserResourceQuotaImpl extends ServiceImpl<UserResourceQuotaMapper, UserResourceQuota> implements UserResourceQuotaService {
    @Autowired
    private Sequence sequence;

    @Override
    public UserResourceQuota getByUserId(String userId) {
        return baseMapper.getByUserId(userId);
    }

    @Override
    public void insertDefaultQuota(String userId) {
        UserResourceQuota quota = new UserResourceQuota();
        quota.setId(sequence.nextNo());
        quota.setUserId(userId);
        quota.setUsedSize(0L);
        quota.setUsedCount(0);
        quota.setMaxFileSize(20L * 1024 * 1024); // 20MB
        quota.setMaxTotalSize(150L * 1024 * 1024); // 150MB
        baseMapper.insert(quota);
    }
}
