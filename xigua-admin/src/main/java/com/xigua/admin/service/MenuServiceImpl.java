package com.xigua.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.admin.mapper.MenuMapper;
import com.xigua.api.service.admin.MenuService;
import com.xigua.api.service.admin.RoleMenuService;
import com.xigua.domain.entity.Menu;
import com.xigua.domain.entity.RoleMenu;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @ClassName MenuServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/22 15:06
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {
    @Autowired
    private RoleMenuService roleMenuService;

    @Override
    public List<Menu> getMenus(List<String> roleIds) {
        List<Menu> menus = List.of();

        if (CollectionUtils.isEmpty(roleIds)) {
            return menus;
        }

        List<RoleMenu> roleMenus = roleMenuService.list(new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, roleIds));
        if(CollectionUtils.isEmpty(roleMenus)){
            return menus;
        }
        Set<String> menuIds = roleMenus.stream().map(RoleMenu::getMenuId).collect(Collectors.toSet());
        if(CollectionUtils.isEmpty(menuIds)){
            return menus;
        }

        menus = baseMapper.selectList(new LambdaQueryWrapper<Menu>().in(Menu::getId, menuIds));
        return menus;
    }
}
