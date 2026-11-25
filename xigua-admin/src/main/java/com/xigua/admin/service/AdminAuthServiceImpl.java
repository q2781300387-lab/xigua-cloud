package com.xigua.admin.service;

import com.xigua.api.service.LoginLogService;
import com.xigua.api.service.UserService;
import com.xigua.api.service.admin.AdminAuthService;
import com.xigua.api.service.admin.MenuService;
import com.xigua.api.service.admin.RoleService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.model.UserToken;
import com.xigua.common.core.thread.ThreadMgr;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.core.util.NetUtil;
import com.xigua.common.core.util.PasswordUtil;
import com.xigua.common.core.util.TokenUtil;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.bo.LoginLogFailBO;
import com.xigua.domain.bo.LoginLogSuccessBO;
import com.xigua.domain.bo.LogoutBO;
import com.xigua.domain.bo.MenuTree;
import com.xigua.domain.dto.admin.LoginDTO;
import com.xigua.domain.entity.Menu;
import com.xigua.domain.entity.Role;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.LoginType;
import com.xigua.domain.enums.MenuType;
import com.xigua.domain.enums.Platform;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.vo.admin.AdminUserInfoVO;
import com.xigua.domain.vo.admin.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @ClassName AuthServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/22 11:08
 */
@Slf4j
@Service
@DubboService
public class AdminAuthServiceImpl implements AdminAuthService {
    // token 过期时间为3小时
    private static long TOKEN_TTL = 60 * 60 * 3;

    @Autowired
    private RedisClient redisClient;
    @Autowired
    private RoleService roleService;
    @Autowired
    private MenuService menuService;

    @DubboReference
    private UserService userService;
    @DubboReference
    private LoginLogService loginLogService;


    @Override
    public LoginVO login(LoginDTO dto) {
        LoginVO loginVO = new LoginVO();
        String username = dto.getUsername();
        String password = dto.getPassword();
        String captcha = dto.getCaptcha();
        String uuid = dto.getUuid();

        if(StringUtils.isEmpty(username)){
            throw new BusinessException("用户名不能为空");
        }

        if(StringUtils.isEmpty(password)){
            throw new BusinessException("密码不能为空");
        }

        if(StringUtils.isEmpty(captcha)){
            throw new BusinessException("验证码不能为空");
        }

        if(StringUtils.isEmpty(uuid)){
            throw new BusinessException("验证码id不能为空");
        }

        String redisKey = RedisEnum.CAPTCHA.getKey() + uuid;
        String value = redisClient.get(redisKey);
        if(StringUtils.isEmpty(value)){
            throw new BusinessException("验证码已失效");
        }

        if (!value.equalsIgnoreCase(captcha)) {
            // todo 可以记录错误次数，防止暴力破解
            throw new BusinessException("验证码错误，请重新输入");
        }

        // 删除验证码
        redisClient.del(redisKey);

        User user = userService.getByUsername(username);
        if(user == null){
            loginFail(username, "用户名不存在");
            throw new BusinessException("用户名不存在");
        }

        // todo 前端密文提交密码
        if(!PasswordUtil.matches(password, user.getPassword())){
            loginFail(username, "密码错误");
            throw new BusinessException("密码错误");
        }

        // 判断是否允许登录管理端
        if(user.getIsAdmin() == null || user.getIsAdmin() != 1){
            loginFail(username, "该用户无管理端登录权限");
            throw new BusinessException("该用户无管理端登录权限");
        }

        // 生成token
        String token = createToken(user);
        loginVO.setToken(token);
        LocalDateTime expireAt = LocalDateTime.now().plus(TOKEN_TTL, ChronoUnit.SECONDS);
        loginVO.setExpireAt(expireAt);

        // 保存token到redis
        redisClient.set(RedisEnum.TOKEN.getKey() + user.getId(), token, TOKEN_TTL);

        // 历史无盐 MD5 密码：本次校验已通过，顺带升级为 BCrypt（失败不影响登录）
        if(PasswordUtil.isLegacyHash(user.getPassword())){
            try {
                userService.updatePasswordById(user.getId(), password);
                log.info("----------->>>>>>> 管理端登录：历史密码已升级为BCrypt userId={}", user.getId());
            } catch (Exception e) {
                log.warn("管理端历史密码升级BCrypt失败 userId={}", user.getId(), e);
            }
        }

        Executor executor = ThreadMgr.getExecutor();
        executor.execute(() -> {
            loginSuccess(user.getId());
        });
        return loginVO;
    }

    @Override
    public void logout(String userId) {
        redisClient.del(RedisEnum.TOKEN.getKey() + userId);

        Executor executor = ThreadMgr.getExecutor();
        executor.execute(() -> {
            logout();
        });
    }

    @Override
    public String createToken(User user) {
        UserToken userToken = UserToken.builder()
                .userId(user.getId())
                .userName(user.getUsername())
                .phone(user.getPhone())
                .build();
        return TokenUtil.genToken(userToken);
    }

    @Override
    public AdminUserInfoVO userInfo(String userId) {
        AdminUserInfoVO info = new AdminUserInfoVO();
        info.setPermissions(new ArrayList<>());
        info.setRoles(new ArrayList<>());
        info.setRoleNames(new ArrayList<>());

        User user = userService.getById(userId);
        if(user == null){
            throw new BusinessException("用户不存在");
        }
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setPhone(user.getPhone());
        info.setAvatar(user.getAvatar());
        info.setEmail(user.getEmail());

        // 获取角色
        List<Role> roles = roleService.getRoles(userId);
        List<String> roleIds = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(roles)){
            List<String> roleNames = new ArrayList<>();
            List<String> roleKeys = new ArrayList<>();
            for (Role role : roles) {
                roleIds.add(role.getId());
                roleNames.add(role.getRoleName());
                roleKeys.add(role.getRoleKey());
            }
            info.setRoles(roleKeys);
            info.setRoleNames(roleNames);
        }

        // 获取权限
        List<Menu> menus = menuService.getMenus(roleIds);
        if(CollectionUtils.isNotEmpty(menus)){
            List<String> perms = menus.stream().map(Menu::getPerms).collect(Collectors.toList());
            info.setPermissions(perms);
        }

        return info;
    }

    @Override
    public List<MenuTree> userRoute(String userId) {
        // 1. 查询用户角色
        List<Role> roles = roleService.getRoles(userId);
        if (CollectionUtils.isEmpty(roles)) {
            return Collections.emptyList();
        }

        List<String> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toList());

        // 2. 查询角色拥有的菜单
        List<Menu> menus = menuService.getMenus(roleIds);
        if (CollectionUtils.isEmpty(menus)) {
            return Collections.emptyList();
        }

        // 3. 过滤有效的目录 / 菜单
        List<Menu> validMenus = menus.stream()
                .filter(menu ->
                        menu.getMenuType() != null
                                && (menu.getMenuType() == MenuType.DIRECTORY.getValue() || menu.getMenuType() == MenuType.MENU.getValue())
                                && menu.getStatus() != null
                                && menu.getStatus() == 1
                )
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(validMenus)) {
            return Collections.emptyList();
        }

        // 4. Menu -> MenuTree 映射
        Map<String, MenuTree> menuMap = validMenus.stream()
                .map(this::convertToMenuTree)
                .collect(Collectors.toMap(
                        MenuTree::getId,
                        menu -> menu,
                        (a, b) -> a
                ));

        // 5. 构建树
        List<MenuTree> rootMenus = new ArrayList<>();

        for (MenuTree menu : menuMap.values()) {
            String parentId = menu.getParentId();

            if (parentId == null || "0".equals(parentId)) {
                rootMenus.add(menu);
            } else {
                MenuTree parent = menuMap.get(parentId);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(menu);
                }
            }
        }

        // 6. 排序
        sortMenus(rootMenus);

        return rootMenus;
    }

    private void loginSuccess(String userId){
        LoginLogSuccessBO logSuccessBO = new LoginLogSuccessBO();
        logSuccessBO.setUserId(userId);
        logSuccessBO.setLoginIp(NetUtil.getClientIp());
        logSuccessBO.setPlatform(Platform.ADMIN_WEB.getType());
        logSuccessBO.setLoginType(LoginType.PASSWORD.getType());
        // 登录没有token，无法从token中获取，要去request中获取
        logSuccessBO.setUserAgent(getUserAgent());
        loginLogService.recordLoginSuccess(logSuccessBO);
    }

    private void loginFail(String username, String failMsg){
        LoginLogFailBO logFailBO = new LoginLogFailBO();
        logFailBO.setUserId(username);
        logFailBO.setLoginIp(NetUtil.getClientIp());
        logFailBO.setPlatform(Platform.ADMIN_WEB.getType());
        logFailBO.setLoginType(LoginType.PASSWORD.getType());
        logFailBO.setFailMsg(failMsg);
        // 登录没有token，无法从token中获取，要去request中获取
        logFailBO.setUserAgent(getUserAgent());
        loginLogService.recordLoginFail(logFailBO);
    }

    private void logout(){
        LogoutBO logoutBO = new LogoutBO();
        logoutBO.setUserId(UserContext.get().getUserId());
        logoutBO.setLoginIp(NetUtil.getClientIp());
        logoutBO.setPlatform(Platform.ADMIN_WEB.getType());
        logoutBO.setUserAgent(UserContext.get().getUserAgent());
        loginLogService.recordLogout(logoutBO);
    }

    public static String getUserAgent() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        if (requestAttributes instanceof ServletRequestAttributes) {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
            HttpServletRequest request = servletRequestAttributes.getRequest();

            // 获取 User-Agent
            return request.getHeader("User-Agent");
        }

        return null;
    }

    private MenuTree convertToMenuTree(Menu menu) {
        MenuTree tree = new MenuTree();

        tree.setId(menu.getId());
        tree.setParentId(
                menu.getParentId() == null ? "0" : String.valueOf(menu.getParentId())
        );
        tree.setMenuName(menu.getMenuName());
        tree.setMenuType(menu.getMenuType());
        tree.setPath(menu.getPath());
        tree.setPerms(menu.getPerms());
        tree.setMenuSort(menu.getMenuSort());
        tree.setStatus(menu.getStatus());

        // 下面这些如果你 Menu 表里没有，可以先留空
        tree.setName(menu.getMenuName());
        if(menu.getParentId() == null || 0 == menu.getParentId()){
            tree.setComponent("Layout");
        }else{
            tree.setComponent(menu.getComponent());
        }
        tree.setIcon(menu.getIcon());
        tree.setIsExternal(false);
        tree.setIsCache(true);
        tree.setIsHidden(false);
        tree.setChildren(new ArrayList<>());

        return tree;
    }

    /**
     * 递归对菜单进行排序
     */
    private void sortMenus(List<MenuTree> menus) {
        if (menus == null || menus.isEmpty()) {
            return;
        }

        // 按菜单排序字段排序
        menus.sort(Comparator.comparing(MenuTree::getMenuSort, Comparator.nullsLast(Comparator.naturalOrder())));

        // 递归排序子菜单
        for (MenuTree menu : menus) {
            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                sortMenus(menu.getChildren());
            }
        }
    }
}
