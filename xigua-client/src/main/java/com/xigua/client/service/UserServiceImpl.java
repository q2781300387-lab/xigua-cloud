package com.xigua.client.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.client.mapper.UserMapper;
import com.xigua.client.util.PasswordPolicy;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.PasswordUtil;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.dto.QueryUserPageDTO;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.common.core.model.UserToken;
import com.xigua.common.mybatis.base.PagePlus;
import com.xigua.domain.vo.UserSearchVO;
import com.xigua.api.service.CenterService;
import com.xigua.api.service.EmailService;
import com.xigua.api.service.FriendRelationService;
import com.xigua.api.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName UserServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2025/4/27 9:53
 */
@Slf4j
@Service
@DubboService
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private Sequence sequence;
    @DubboReference
    private EmailService emailService;
    @DubboReference
    private CenterService centerService;
    @DubboReference
    private FriendRelationService friendRelationService;

    /**
     * 获取当前登录用户信息
     * @author wangjinfei
     * @date 2025/5/10 12:52
     * @return User
     */
    @Override
    public User getUserInfo() {
        UserToken userToken = UserContext.get();
        User user = baseMapper.selectById(userToken.getUserId());
        // 用户信息接口不返回密码哈希，避免前端缓存或日志泄露
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    /**
     * 上传头像
     * @author wangjinfei
     * @date 2025/5/10 20:37
     * @param avatar
     * @return String
     */
    @Override
    public Boolean uploadAvatar(String avatar) {
        UserToken userToken = UserContext.get();
        String userId = userToken.getUserId();
        baseMapper.update(new User(), new LambdaUpdateWrapper<User>().eq(User::getId, userId)
                .set(User::getAvatar, avatar));

        // 缓存用户信息
        User user = baseMapper.selectById(userId);
        redisClient.set(RedisEnum.USER.getKey() + userId, JSONObject.toJSONString(user));
        return true;
    }

    /**
     * 更新用户信息
     * @author wangjinfei
     * @date 2025/5/11 20:34
     * @param user
     * @return Boolean
     */
    @Override
    public Boolean updateUserInfo(User user) {
        UserToken userToken = UserContext.get();
        String userId = userToken.getUserId();
        String originalUserName = userToken.getUserName();
        String username = user.getUsername();

        if("admin".equals(originalUserName)){
            throw new BusinessException("管理员不能修改用户信息");
        }

        // 用户名根之前不一样在校验
        if(!originalUserName.equals(username)){
            // 用户名是否存在
            Long usernameCount = baseMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, username));
            if(usernameCount > 0){
                throw new BusinessException("用户名已存在");
            }
        }

        user.setId(userId);
        int i = baseMapper.updateById(user);

        // 更新缓存
        redisClient.set(RedisEnum.USER.getKey() + user.getId(), JSONObject.toJSONString(user));
        return i > 0;
    }

    /**
     * 管理端更新用户信息
     * @author wangjinfei
     * @date 2026/3/10
     * @param user
     * @return Boolean
     */
    @Override
    public Boolean updateUserInfoForAdmin(User user) {
        if (user == null || StringUtils.isEmpty(user.getId())) {
            throw new BusinessException("用户ID不能为空");
        }

        int i = baseMapper.updateById(user);

        if (i > 0) {
            User updatedUser = baseMapper.selectById(user.getId());
            if (updatedUser != null) {
                redisClient.set(RedisEnum.USER.getKey() + user.getId(), JSONObject.toJSONString(updatedUser));
            }
        }

        return i > 0;
    }

    /**
     * 根据用户名查询用户列表
     * @author wangjinfei
     * @date 2025/5/12 21:18
     * @param username
     * @return List<User>
     */
    @Override
    public List<UserSearchVO> getListByName(String username) {
        List<UserSearchVO> userList = new ArrayList<>();

        String myselfId = UserContext.get().getUserId();
        // 根据用户名模糊查询且排除自己
        userList = baseMapper.getListByName(username, myselfId);

        // 获取用户连接状态和好友关系
        for (UserSearchVO user : userList) {
            user.setIsOnline(centerService.isOnline(user.getId()));
            user.setIsFriend(friendRelationService.isFriend(myselfId, user.getId()));
        }

        return userList;
    }

    @Override
    public List<UserSearchVO> getListByNameForAdmin(String username) {
        List<UserSearchVO> userList = new ArrayList<>();
        if (StringUtils.isEmpty(username)) {
            return userList;
        }

        userList = baseMapper.getListByName(username, "-1");
        return userList;
    }

    /**
     * 根据id列表查询用户列表
     * @author wangjinfei
     * @date 2025/5/13 23:36
     * @param ids
     * @return List<User>
     */
    @Override
    public List<User> getListByIds(List<String> ids) {
        List<User> userList = new ArrayList<>();
        if(CollectionUtils.isEmpty(ids)){
            return userList;
        }

        userList = baseMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getId, ids));
        return userList;
    }

    /**
     * 根据用户名获取数量
     * @author wangjinfei
     * @date 2025/6/12 21:51
     * @param username
     * @return Long
     */
    @Override
    public Long getCountByName(String username) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return count;
    }

    /**
     * 根据邮箱获取数量
     * @author wangjinfei
     * @date 2025/6/12 21:53
     * @param email
     * @return Long
     */
    @Override
    public Long getCountByEmail(String email) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        return count;
    }

    /**
     * 根据用户名获取用户
     * @author wangjinfei
     * @date 2025/6/12 21:54
     * @param username
     * @return User
     */
    @Override
    public User getByUsername(String username) {
        User user = baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .or()
                .eq(User::getEmail, username));
        return user;
    }

    /**
     * 根据id获取用户
     * @author wangjinfei
     * @date 2025/7/6 11:50
     * @param id
     * @return User
     */
    @Override
    public User getById(String id) {
        // 先查询缓存
        String userCache = redisClient.get(RedisEnum.USER.getKey() + id);
        if(StringUtils.isNotEmpty(userCache)){
            User user = JSONObject.parseObject(userCache, User.class);
            return user;
        }

        // 缓存不存在查询数据库
        User user = baseMapper.selectById(id);
        return user;
    }

    /**
     * 添加用户到redis
     * @author wangjinfei
     * @date 2025/7/6 16:02
     * @param userId
     */
    @Override
    public Boolean addUser2Redis(String userId) {
        if(StringUtils.isEmpty(userId)){
            return false;
        }

        User user = baseMapper.selectById(userId);
        if(user == null){
            return false;
        }

        redisClient.set(RedisEnum.USER.getKey() + userId, JSONObject.toJSONString(user));
        return true;
    }

    /**
     * 获取所有用户id
     * @author wangjinfei
     * @date 2025/7/29 17:40
     * @return List<String>
     */
    @Override
    public List<String> getAllUserId() {
        return baseMapper.getAllUserId();
    }

    @Override
    public PagePlus<User> page(QueryUserPageDTO dto) {
        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        // 用户名筛选
        String username = dto.getUsername();
        if(StringUtils.isNotEmpty(username)){
            queryWrapper.like(User::getUsername, username);
        }
        // 创建时间筛选（yyyy-MM-dd 格式，查询当天创建的记录）
        String createTime = dto.getCreateTime();
        if(StringUtils.isNotEmpty(createTime)){
            try {
                // 当天起始时间 00:00:00
                String startTime = createTime + " 00:00:00";
                // 当天结束时间 23:59:59
                String endTime = createTime + " 23:59:59";
                queryWrapper.ge(User::getCreateTime, startTime);
                queryWrapper.le(User::getCreateTime, endTime);
            } catch (Exception e) {
                log.warn("创建时间格式错误: {}", createTime, e);
            }
        }
        queryWrapper.orderByDesc(User::getCreateTime);
        Page<User> userPage = baseMapper.selectPage(page, queryWrapper);
        return PagePlus.fromPage(userPage);
    }

    @Override
    public Long countTotalUsers() {
        return this.count(new LambdaQueryWrapper<User>().eq(User::getDelFlag, 0));
    }

    @Override
    public Long countUsersByTime(Long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()
        );
        return this.count(new LambdaQueryWrapper<User>()
            .eq(User::getDelFlag, 0)
            .le(User::getCreateTime, dateTime)
        );
    }

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
    @Override
    public Boolean resetPassword(String email, String resetToken, String newPassword, String confirmPassword) {
        // 1. 从 Redis 中验证 resetToken
        String key = RedisEnum.RESET_PASSWORD_TOKEN.getKey() + email;
        String cachedToken = redisClient.get(key);
        if(StringUtils.isEmpty(cachedToken)){
            throw new BusinessException("重置密码token已过期，请重新获取");
        }
        if(!cachedToken.equals(resetToken)){
            throw new BusinessException("重置密码token无效");
        }

        // 2. 校验两次密码是否一致
        if(!newPassword.equals(confirmPassword)){
            throw new BusinessException("两次密码输入不一致");
        }

        // 2.1 校验密码强度（与注册共用同一套规则）
        String pwdError = PasswordPolicy.check(newPassword, null);
        if(pwdError != null){
            throw new BusinessException(pwdError);
        }

        // 3. 根据邮箱查询用户
        User user = baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if(user == null){
            throw new BusinessException("用户不存在");
        }

        // 4. 加密新密码（BCrypt，自带随机盐）
        String encryptedPassword = PasswordUtil.encode(newPassword);

        // 5. 更新数据库中的密码
        int updateCount = baseMapper.update(new User(), new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getPassword, encryptedPassword));

        if(updateCount == 0){
            throw new BusinessException("密码重置失败");
        }

        // 6. 删除 Redis 中的 resetToken（防止重复使用）
        redisClient.del(key);

        // 7. 更新用户缓存
        User updatedUser = baseMapper.selectById(user.getId());
        redisClient.set(RedisEnum.USER.getKey() + user.getId(), JSONObject.toJSONString(updatedUser));

        return true;
    }

    /**
     * 按用户 ID 重设密码：BCrypt 加密、落库并刷新缓存。
     * 主要用于登录流程把历史 MD5 密码升级为 BCrypt。
     */
    @Override
    public Boolean updatePasswordById(String userId, String rawPassword) {
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(rawPassword)) {
            throw new BusinessException("用户ID或密码不能为空");
        }

        int updateCount = baseMapper.update(new User(), new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, PasswordUtil.encode(rawPassword)));

        if (updateCount == 0) {
            throw new BusinessException("密码更新失败");
        }

        User updatedUser = baseMapper.selectById(userId);
        if (updatedUser != null) {
            redisClient.set(RedisEnum.USER.getKey() + userId, JSONObject.toJSONString(updatedUser));
        }
        return true;
    }

    @Override
    public Boolean updateModeration(String userId, Integer status,
                                    LocalDateTime muteUntil, String reason) {
        if (StringUtils.isEmpty(userId)) {
            throw new BusinessException("用户ID不能为空");
        }
        if (status == null || status < 0 || status > 2) {
            throw new BusinessException("账号状态不合法");
        }

        User existing = baseMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        if (Integer.valueOf(1).equals(existing.getIsAdmin())) {
            throw new BusinessException("管理员账号不允许封禁或禁言");
        }

        int updateCount = baseMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, status)
                .set(User::getMuteUntil, muteUntil)
                .set(User::getStatusReason, StringUtils.trimToNull(reason))
                .set(User::getStatusUpdateTime, LocalDateTime.now()));

        if (updateCount == 0) {
            throw new BusinessException("账号状态更新失败");
        }

        User updated = baseMapper.selectById(userId);
        if (updated != null) {
            redisClient.set(RedisEnum.USER.getKey() + userId, JSONObject.toJSONString(updated));
        }

        // 封禁后立即踢掉已有登录态；禁言保留登录态，只拦截发消息
        if (Integer.valueOf(2).equals(status)) {
            redisClient.del(RedisEnum.TOKEN.getKey() + userId);
        }

        return true;
    }
}
