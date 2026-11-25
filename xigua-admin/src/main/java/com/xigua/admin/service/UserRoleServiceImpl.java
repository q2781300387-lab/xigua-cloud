package com.xigua.admin.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.admin.mapper.UserRoleMapper;
import com.xigua.api.service.admin.UserRoleService;
import com.xigua.domain.entity.UserRole;
import org.springframework.stereotype.Service;

/**
 * @ClassName UserRoleServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/22 15:06
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {
}
