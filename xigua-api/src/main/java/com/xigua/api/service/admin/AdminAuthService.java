package com.xigua.api.service.admin;

import com.xigua.domain.bo.MenuTree;
import com.xigua.domain.dto.admin.LoginDTO;
import com.xigua.domain.entity.User;
import com.xigua.domain.vo.admin.AdminUserInfoVO;
import com.xigua.domain.vo.admin.LoginVO;

import java.util.List;

/**
 * @ClassName AuthService
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/22 11:08
 */
public interface AdminAuthService {
    /**
     * 登录
     * @author wangjinfei
     * @date 2026/1/22 11:23
     * @param dto
     * @return LoginVO
    */
    LoginVO login(LoginDTO dto);

    /**
     * 登出
     * @author wangjinfei
     * @date 2026/1/22 11:21
     * @param userId
    */
    void logout(String userId);

    /**
     * 创建token
     * @author wangjinfei
     * @date 2026/1/22 11:21
     * @param user
     * @return String
    */
    String createToken(User user);

    /**
     * 获取用户信息
     * @author wangjinfei
     * @date 2026/1/22 20:14
     * @param userId
     * @return AdminUserInfoVO
    */
    AdminUserInfoVO userInfo(String userId);

    /**
     * 获取用户权限
     * @author wangjinfei
     * @date 2026/1/22 20:28
     * @param userId
    */
    List<MenuTree> userRoute(String userId);
}
