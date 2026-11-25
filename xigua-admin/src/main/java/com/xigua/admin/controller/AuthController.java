package com.xigua.admin.controller;

import com.xigua.api.service.admin.AdminAuthService;
import com.xigua.common.core.base.R;
import com.xigua.common.core.util.UserContext;
import com.xigua.domain.dto.admin.LoginDTO;
import com.xigua.domain.vo.admin.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName AuthController
 * @Description 管理端认证接口
 * @Author wangjinfei
 * @Date 2026/1/22 11:06
 */
@Tag(name = "管理端认证接口", description = "提供管理端登录、登出、用户信息及权限查询功能")
@RequestMapping("/auth")
@RestController
public class AuthController {
    @Autowired
    private AdminAuthService authService;

    @Operation(summary = "登录", description = "管理端用户登录，返回访问令牌")
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody LoginDTO dto){
        return R.ok(authService.login(dto), "登陆成功");
    }

    @Operation(summary = "登出", description = "管理端用户登出，清除令牌")
    @PostMapping("/logout")
    public R logout(@Parameter(description = "用户ID", required = true) @RequestParam("userId") String userId){
        authService.logout(userId);
        return R.ok(null, "登出成功");
    }

    @Operation(summary = "用户信息", description = "获取当前登录用户的基本信息")
    @GetMapping("/user/info")
    public R userInfo(){
        String userId = UserContext.get().getUserId();
        return R.ok(authService.userInfo(userId));
    }

    @Operation(summary = "用户权限", description = "获取当前登录用户的菜单路由权限")
    @GetMapping("/user/route")
    public R userRoute(){
        String userId = UserContext.get().getUserId();
        return R.ok(authService.userRoute(userId));
    }
}
