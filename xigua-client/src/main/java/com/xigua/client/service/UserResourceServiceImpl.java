package com.xigua.client.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.api.service.UserResourceService;
import com.xigua.client.mapper.UserResourceMapper;
import com.xigua.domain.entity.UserResource;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

/**
 * @ClassName UserResourceServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/27 11:15
 */
@Service
@DubboService
public class UserResourceServiceImpl extends ServiceImpl<UserResourceMapper, UserResource> implements UserResourceService {
}
