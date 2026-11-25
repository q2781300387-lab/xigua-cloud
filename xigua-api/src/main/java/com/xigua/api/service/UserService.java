package com.xigua.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.domain.dto.QueryUserPageDTO;
import com.xigua.domain.entity.User;
import com.xigua.common.mybatis.base.PagePlus;
import com.xigua.domain.vo.UserSearchVO;

import java.util.List;

/**
 * @ClassName UserService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/3/18 20:44
 */
public interface UserService extends IService<User> {

    /**
     * 获取当前登录用户信息
     * @author wangjinfei
     * @date 2025/5/10 12:52
     * @return User
    */
    User getUserInfo();

    /**
     * 上传头像
     * @author wangjinfei
     * @date 2025/5/10 20:37
     * @param avatar
     * @return String
    */
    Boolean uploadAvatar(String avatar);

    /**
     * 更新用户信息
     * @author wangjinfei
     * @date 2025/5/11 20:34
     * @param user
     * @return Boolean
    */
    Boolean updateUserInfo(User user);

    /**
     * 管理端更新用户信息
     * @author wangjinfei
     * @date 2026/3/10
     * @param user
     * @return Boolean
    */
    Boolean updateUserInfoForAdmin(User user);

    /**
     * 根据用户名查询用户列表
     * @author wangjinfei
     * @date 2025/5/12 21:18
     * @param username
     * @return List<User>
    */
    List<UserSearchVO> getListByName(String username);

    /**
     * 管理端根据用户名查询用户列表
     * @author wangjinfei
     * @date 2026/3/10
     * @param username
     * @return List<UserSearchVO>
     */
    List<UserSearchVO> getListByNameForAdmin(String username);

    /**
     * 根据id列表查询用户列表
     * @author wangjinfei
     * @date 2025/5/13 23:36
     * @param ids
     * @return List<User>
    */
    List<User> getListByIds(List<String> ids);

    /**
     * 根据用户名获取数量
     * @author wangjinfei
     * @date 2025/6/12 21:51
     * @param username
     * @return Long
    */
    Long getCountByName(String username);

    /**
     * 根据邮箱获取数量
     * @author wangjinfei
     * @date 2025/6/12 21:53
     * @param email
     * @return Long
    */
    Long getCountByEmail(String email);

    /**
     * 根据用户名获取用户
     * @author wangjinfei
     * @date 2025/6/12 21:54
     * @param username
     * @return User
    */
    User getByUsername(String username);

    /**
     * 根据id获取用户
     * @author wangjinfei
     * @date 2025/7/6 11:50
     * @param id
     * @return User
    */
    User getById(String id);

    /**
     * 添加用户到redis
     * @author wangjinfei
     * @date 2025/7/6 16:02
     * @param userId
    */
    Boolean addUser2Redis(String userId);

    /**
     * 获取所有用户id
     * @author wangjinfei
     * @date 2025/7/29 17:40
     * @return List<String>
    */
    List<String> getAllUserId();

    /**
     * 分页查询
     * @author wangjinfei
     * @date 2026/2/1 17:45
     * @param dto
     * @return List<User>
    */

    PagePlus<User> page(QueryUserPageDTO dto);

    /**
     * 获取总注册用户数
     * @author wangjinfei
     * @date 2026/3/9
     * @return Long
    */
    Long countTotalUsers();

    /**
     * 根据时间戳获取用户数
     * @author wangjinfei
     * @date 2026/3/9
     * @param timestamp 时间戳（毫秒）
     * @return Long
    */
    Long countUsersByTime(Long timestamp);

    /**
     * 重置密码
     * @author wangjinfei
     * @date 2026/4/13
     * @param email 邮箱地址
     * @param resetToken 重置密码临时token
     * @param newPassword 新密码
     * @param confirmPassword 确认密码
     * @return Boolean
    */
    Boolean resetPassword(String email, String resetToken, String newPassword, String confirmPassword);

    /**
     * 按用户 ID 重设密码（内部用 BCrypt 加密并刷新缓存），
     * 供登录流程把历史 MD5 密码顺带升级为 BCrypt
     * @author wangjinfei
     * @date 2026/11/11
     * @param userId 用户ID
     * @param rawPassword 新密码明文
     * @return Boolean
     */
    Boolean updatePasswordById(String userId, String rawPassword);

    /**
     * 管理端更新账号状态（正常/禁言/封禁），同时刷新用户缓存；
     * 封禁时会删除登录 token，让已登录会话立即失效。
     */
    Boolean updateModeration(String userId, Integer status,
                             java.time.LocalDateTime muteUntil, String reason);
}
