package com.xigua.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.admin.mapper.RoleMapper;
import com.xigua.api.service.admin.RoleService;
import com.xigua.api.service.admin.UserRoleService;
import com.xigua.domain.entity.Role;
import com.xigua.domain.entity.UserRole;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @ClassName RoleServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/22 15:06
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
    @Autowired
    private UserRoleService userRoleService;

    @Override
    public List<Role> getRoles(String userId) {
        List<Role> roles = List.of();

        List<UserRole> userRoles = userRoleService.list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if(CollectionUtils.isEmpty(userRoles)){
            return roles;
        }
        Set<String> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toSet());

        roles = baseMapper.selectList(new LambdaQueryWrapper<Role>().in(Role::getId, roleIds));
        return roles;
    }
}
