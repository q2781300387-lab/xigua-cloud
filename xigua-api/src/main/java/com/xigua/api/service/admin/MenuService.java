package com.xigua.api.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.domain.entity.Menu;

import java.util.List;

/**
 * @ClassName MenuService
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/22 14:59
 */
public interface MenuService extends IService<Menu> {

    /**
     * 获取菜单
     * @author wangjinfei
     * @date 2026/1/22 20:49
     * @param roleIds
     * @return List<Menu>
    */
    List<Menu> getMenus(List<String> roleIds);
}
