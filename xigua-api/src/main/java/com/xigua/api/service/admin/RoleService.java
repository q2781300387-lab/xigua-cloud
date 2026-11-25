package com.xigua.api.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.domain.entity.Role;

import java.util.List;

/**
 * @ClassName RoleService
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/22 14:58
 */
public interface RoleService extends IService<Role> {
    /**
     * 获取角色
     * @author wangjinfei
     * @date 2026/1/22 20:43
     * @param userId
     * @return List<Role>
    */
    List<Role> getRoles(String userId);
}
