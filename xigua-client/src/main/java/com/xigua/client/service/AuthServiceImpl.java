package com.xigua.client.service;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.*;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.model.UserToken;
import com.xigua.common.core.thread.ThreadMgr;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.core.util.NetUtil;
import com.xigua.common.core.util.PasswordUtil;
import com.xigua.common.core.util.TokenUtil;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.client.util.PasswordPolicy;
import com.xigua.domain.bo.LoginLogFailBO;
import com.xigua.domain.bo.LoginLogSuccessBO;
import com.xigua.domain.bo.LogoutBO;
import com.xigua.domain.dto.LoginDTO;
import com.xigua.domain.dto.RegisterUserDTO;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.LoginType;
import com.xigua.domain.enums.Platform;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @ClassName AuthServiceImpl
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/6/12 21:05
 */
@Slf4j
@Service
@DubboService
public class AuthServiceImpl implements AuthService {
    // token 过期时间为3小时
    private static long TOKEN_TTL = 60 * 60 * 3;
    // 连续失败 5 次后要求验证码，统计窗口 15 分钟
    private static final int LOGIN_FAIL_LIMIT = 5;
    private static final long LOGIN_FAIL_WINDOW = 15 * 60;
    private static final String CAPTCHA_KEY = "LOGIN:CAPTCHA:";

    @Autowired
    private RedisClient redisClient;
    @Autowired
    private Sequence sequence;
    @Autowired
    private LoginLogService loginLogService;
    @Autowired
    private UserResourceQuotaService userResourceQuotaService;

    @DubboReference
    private UserService userService;

    @DubboReference
    private EmailService emailService;

    @Value("${default.avatar}")
    private String defaultAvatar;

    /**
     * 注册
     * @author wangjinfei
     * @date 2025/4/27 9:53
     * @param dto
     * @return Boolean
     */
    @Override
    public Boolean register(RegisterUserDTO dto) {
        String email = dto.getEmail();
        String username = dto.getUsername();
        String code = dto.getCode();

        // 密码强度校验（放在最前面，避免用「用户名已存在」掩盖密码问题，也避免无意义的验证码校验）
        String pwdError = PasswordPolicy.check(dto.getPassword(), username);
        if (pwdError != null) {
            throw new BusinessException(pwdError);
        }

        // 用户名是否存在
        Long usernameCount = userService.getCountByName(username);
        if(usernameCount > 0){
            throw new BusinessException("用户名已存在");
        }

        // 邮箱是否存在
        Long emailCount = userService.getCountByEmail(email);
        if(emailCount > 0){
            throw new BusinessException("邮箱已存在");
        }

        // 校验邮箱
        Boolean checkCode = emailService.checkCode(email, code);
        redisClient.del(RedisEnum.EMAIL_CODE.getKey() + email);

        User user = new User();
        BeanUtils.copyProperties(dto,user);

        // 设置默认头像
        if(StringUtils.isNotEmpty(defaultAvatar)){
            user.setAvatar(defaultAvatar);
        }else {
            log.error("----------->>>>>>> 默认头像未配置");
        }

        // 密码加密（BCrypt，自带随机盐）
        user.setPassword(PasswordUtil.encode(user.getPassword()));

        String userId = sequence.nextNo();
        user.setId(userId);
        boolean insert = userService.save(user);

        // 缓存用户信息
        redisClient.set(RedisEnum.USER.getKey() + user.getId(), JSONObject.toJSONString(user));

        // 添加默认用户容量
        userResourceQuotaService.insertDefaultQuota(userId);

        return insert;
    }

    /**
     * 登录
     * @author wangjinfei
     * @date 2025/5/7 13:40
     * @param loginDTO
     * @return Boolean
     */
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        LoginVO loginVO = new LoginVO();
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();
        String ip = NetUtil.getClientIp();

        long failCount = loginFailCount(username, ip);
        if (failCount >= LOGIN_FAIL_LIMIT) {
            verifyCaptcha(loginDTO, failCount);
        }

        User user = userService.getByUsername(username);
        if(user == null){
            loginFail(username, "用户名不存在");
            markLoginFail(username, ip);
            throw new BusinessException("用户名不存在或密码错误");
        }
        if(!PasswordUtil.matches(password, user.getPassword())){
            loginFail(username, "密码错误");
            markLoginFail(username, ip);
            throw new BusinessException("用户名不存在或密码错误");
        }
        if (Integer.valueOf(2).equals(user.getStatus())) {
            loginFail(username, "账号已被封禁");
            throw new BusinessException("账号已被封禁，请联系管理员");
        }
        if (Integer.valueOf(3).equals(user.getStatus())) {
            loginFail(username, "账号已注销");
            throw new BusinessException("账号已注销，无法登录");
        }
        clearLoginFail(username, ip);

        // 生成token
        String token = createToken(user);
        loginVO.setToken(token);
        LocalDateTime expireAt = LocalDateTime.now().plus(TOKEN_TTL, ChronoUnit.SECONDS);
        loginVO.setExpireAt(expireAt);

        // 保存token到redis：设备级 key 支持多端并存，旧 key 保留最新 token 兼容旧逻辑
        redisClient.set(TokenUtil.deviceTokenKey(user.getId(), token), token, TOKEN_TTL);
        redisClient.set(RedisEnum.TOKEN.getKey() + user.getId(), token, TOKEN_TTL);

        // 刷新用户信息缓存（websocket握手等场景依赖该缓存判断用户是否合法）
        redisClient.set(RedisEnum.USER.getKey() + user.getId(), JSONObject.toJSONString(user));

        // 历史无盐 MD5 密码：本次校验已通过，顺带升级为 BCrypt（失败不影响登录，下次登录重试）
        if(PasswordUtil.isLegacyHash(user.getPassword())){
            try {
                userService.updatePasswordById(user.getId(), password);
                log.info("----------->>>>>>> 历史密码已升级为BCrypt userId={}", user.getId());
            } catch (Exception e) {
                log.warn("历史密码升级BCrypt失败 userId={}", user.getId(), e);
            }
        }

        Executor executor = ThreadMgr.getExecutor();
        executor.execute(() -> {
            loginSuccess(user.getId());
        });
        return loginVO;
    }

    /**
     * 登出
     * @author wangjinfei
     * @date 2026/1/4 20:10
     * @param userId
     */
    @Override
    public void logout(String userId) {
        redisClient.del(RedisEnum.TOKEN.getKey() + userId);

        Executor executor = ThreadMgr.getExecutor();
        executor.execute(() -> {
            logout();
        });
    }

    @Override
    public void logoutCurrent(String userId, String token) {
        if (StringUtils.isNotBlank(token)) {
            redisClient.del(TokenUtil.deviceTokenKey(userId, token));
            // 旧 key 只在它恰好存的是当前 token 时才删，避免把其他设备踢下线
            String legacy = redisClient.get(RedisEnum.TOKEN.getKey() + userId);
            if (token.equals(legacy)) {
                redisClient.del(RedisEnum.TOKEN.getKey() + userId);
            }
        }

        Executor executor = ThreadMgr.getExecutor();
        executor.execute(() -> {
            logout();
        });
    }

    /**
     * 创建token
     * @author wangjinfei
     * @date 2025/5/10 12:20
     * @param user
     * @return String
     */
    @Override
    public String createToken(User user) {
        UserToken userToken = UserToken.builder()
                .userId(user.getId())
                .userName(user.getUsername())
                .phone(user.getPhone())
                .build();
        return TokenUtil.genToken(userToken);
    }

    /**
     * 创建一次性ticket
     * @author wangjinfei
     * @date 2025/6/14 16:07
     * @param userId
     * @return String
     */
    @Override
    public String createTicket(String userId) {
        String ticket = sequence.nextNo();
        // redis存储一次性ticket，有效期1分钟
        redisClient.set(RedisEnum.TICKET.getKey() + ticket, userId, 60);

        return ticket;
    }

    /**
     * ticket兑换token
     * @author wangjinfei
     * @date 2025/6/14 21:46
     * @param ticket
     * @return String
     */
    @Override
    public String redeemToken(String ticket) {
        String key = RedisEnum.TICKET.getKey() + ticket;
        String userId = redisClient.get(key);
        if(userId == null){
            throw new BusinessException("ticket已失效或不存在");
        }

        redisClient.del(key);

        User user = userService.getById(userId);
        if(user == null){
            throw new BusinessException("用户不存在");
        }

        return createToken(user);
    }

    @Override
    public Map<String, Object> loginRisk(String username) {
        Map<String, Object> result = new HashMap<>();
        long count = loginFailCount(username, NetUtil.getClientIp());
        result.put("failCount", count);
        result.put("captchaRequired", count >= LOGIN_FAIL_LIMIT);
        result.put("limit", LOGIN_FAIL_LIMIT);
        return result;
    }

    @Override
    public Map<String, Object> captcha(String username) {
        int a = ThreadLocalRandom.current().nextInt(2, 10);
        int op = ThreadLocalRandom.current().nextInt(0, 2);
        // 减法保证被减数不小于减数，避免出现负数答案导致手机数字键盘无法输入
        int b = op == 0
                ? ThreadLocalRandom.current().nextInt(1, 10)
                : ThreadLocalRandom.current().nextInt(1, a + 1);
        int answer = op == 0 ? a + b : a - b;
        String question = op == 0 ? a + " + " + b + " = ?" : a + " - " + b + " = ?";
        String id = sequence.nextNo();
        redisClient.set(CAPTCHA_KEY + id, String.valueOf(answer), 300);
        Map<String, Object> result = new HashMap<>();
        result.put("captchaId", id);
        result.put("question", question);
        result.put("expireSeconds", 300);
        return result;
    }

    private String failKeyUsername(String username) {
        return "LOGIN:FAIL:USER:" + StringUtils.lowerCase(StringUtils.trimToEmpty(username));
    }

    private String failKeyIp(String ip) {
        return "LOGIN:FAIL:IP:" + StringUtils.defaultString(ip);
    }

    private long loginFailCount(String username, String ip) {
        long userCount = StringUtils.isBlank(username) ? 0 : parseLong(redisClient.getRawLong(failKeyUsername(username)));
        long ipCount = parseLong(redisClient.getRawLong(failKeyIp(ip)));
        return Math.max(userCount, ipCount);
    }

    private long parseLong(Long value) {
        return value == null ? 0 : value;
    }

    private void markLoginFail(String username, String ip) {
        incrFailKey(failKeyUsername(username));
        incrFailKey(failKeyIp(ip));
    }

    private void incrFailKey(String key) {
        try {
            redisClient.incrRaw(key, 1, LOGIN_FAIL_WINDOW);
        } catch (Exception e) {
            log.warn("记录登录失败次数异常 key={}", key, e);
        }
    }

    private void clearLoginFail(String username, String ip) {
        redisClient.del(failKeyUsername(username));
        redisClient.del(failKeyIp(ip));
    }

    private void verifyCaptcha(LoginDTO dto, long failCount) {
        String id = dto.getCaptchaId();
        String code = dto.getCaptchaCode();
        if (StringUtils.isBlank(id) || StringUtils.isBlank(code)) {
            throw new BusinessException("登录失败次数过多，请先完成验证码");
        }
        String key = CAPTCHA_KEY + id;
        // Redis 的 value 序列化会给字符串加双引号，这里统一还原后再比较
        String expected = redisClient.get(key);
        redisClient.del(key);
        expected = expected == null ? null : expected.replace("\"", "").trim();
        if (StringUtils.isBlank(expected) || !expected.equals(StringUtils.trim(code))) {
            throw new BusinessException("验证码错误或已过期");
        }
        log.info("登录风控验证码通过，累计失败次数={}", failCount);
    }

    private void loginSuccess(String userId){
        LoginLogSuccessBO logSuccessBO = new LoginLogSuccessBO();
        logSuccessBO.setUserId(userId);
        logSuccessBO.setLoginIp(NetUtil.getClientIp());
        logSuccessBO.setPlatform(Platform.CLIENT_WEB.getType());
        logSuccessBO.setLoginType(LoginType.PASSWORD.getType());
        // 登录没有token，无法从token中获取，要去request中获取
        logSuccessBO.setUserAgent(getUserAgent());
        loginLogService.recordLoginSuccess(logSuccessBO);
    }

    private void loginFail(String username, String failMsg){
        LoginLogFailBO logFailBO = new LoginLogFailBO();
        logFailBO.setUserId(username);
        logFailBO.setLoginIp(NetUtil.getClientIp());
        logFailBO.setPlatform(Platform.CLIENT_WEB.getType());
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
        logoutBO.setPlatform(Platform.CLIENT_WEB.getType());
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
}
