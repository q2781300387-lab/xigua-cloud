package com.xigua.admin.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.admin.mapper.RoleMenuMapper;
import com.xigua.api.service.admin.RoleMenuService;
import com.xigua.domain.entity.RoleMenu;
import org.springframework.stereotype.Service;

/**
 * @ClassName RoleMenuServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/22 15:06
 */
@Service
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenu> implements RoleMenuService {
}
