package com.xigua.client.service;

import com.xigua.api.service.UserService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.api.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * @ClassName EmailServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2025/5/7 14:42
 */
@Slf4j
@Service
@DubboService
public class EmailServiceImpl implements EmailService {
    // QQ邮箱正则表达式
    private static final String QQ_EMAIL_REGEX = "^[1-9]\\d{4,10}@qq\\.com$";  // QQ号5-11位，不能以0开头

    @Value("${spring.mail.username}")
    private String from;
    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private RedisClient redisClient;
    @DubboReference
    private UserService userService;

    /**
     * 发送邮件
     * @author wangjinfei
     * @date 2025/5/7 14:49
     * @param to 收件人
     * @param subject 邮件主题
     * @param text 邮件内容
     */
    public Boolean send(String to, String subject, String text) {
        // 校验邮箱格式
        if (!isValidQQEmail(to)) {
            throw new BusinessException("邮箱格式错误，请输入正确的 QQ 邮箱");
        }

        // 发送频控：同一邮箱 60 秒内只允许发送一次，防止接口被刷导致邮件轰炸
        String limitKey = RedisEnum.EMAIL_CODE.getKey() + "limit:" + to;
        if (StringUtils.isNotEmpty(redisClient.get(limitKey))) {
            throw new BusinessException("验证码发送过于频繁，请 60 秒后再试");
        }
        // 先占位再发信，避免并发重复发送；发送失败则释放，允许立即重试
        redisClient.set(limitKey, "1", 60);

        subject = "西瓜 - 邮箱验证码";
        String code = RandomStringUtils.randomNumeric(6);
        text = "您的验证码是: " + code + "，有效期为3分钟";

        SimpleMailMessage message = new SimpleMailMessage();
        // 发件人
        message.setFrom(from);
        // 收件人
        message.setTo(to);
        // 邮件主题
        message.setSubject(subject);
        // 邮件内容
        message.setText(text);

        try {
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            redisClient.del(limitKey);
            return false;
        }

        // 将验证码存储到 Redis 中，设置过期时间为 3 分钟
        String key = RedisEnum.EMAIL_CODE.getKey() + to;
        redisClient.set(key, code,180);

        return true;
    }

    private boolean isValidQQEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // 基础验证
        return email.matches(QQ_EMAIL_REGEX);
    }

    /**
     * 校验验证码
     * @author wangjinfei
     * @date 2025/5/7 15:26
     * @param email
     * @param code
     * @return Boolean
     */
    @Override
    public Boolean checkCode(String email, String code) {
        String key = RedisEnum.EMAIL_CODE.getKey() + email;
        String redisCode = redisClient.get(key);
        if(StringUtils.isEmpty(redisCode)){
            throw new BusinessException("验证码已过期，请重新获取");
        }

        if(code.equals(redisCode) == false){
            throw new BusinessException("验证码错误");
        }

        return true;
    }

    /**
     * 验证重置密码验证码
     * @author wangjinfei
     * @date 2026/4/13
     * @param email 邮箱地址
     * @param code 验证码
     * @return String 重置密码临时token
     */
    @Override
    public String verifyResetCode(String email, String code) {
        // 校验邮箱格式
        if (!isValidQQEmail(email)) {
            throw new BusinessException("邮箱格式错误，请输入正确的 QQ 邮箱");
        }

        // 1. 校验邮箱是否已注册
        Long emailCount = userService.getCountByEmail(email);
        if(emailCount == 0){
            throw new BusinessException("该邮箱未注册");
        }

        // 2. 校验验证码
        Boolean checkResult = this.checkCode(email, code);
        if(!checkResult){
            throw new BusinessException("验证码错误或已过期");
        }

        // 3. 生成临时 resetToken（UUID）
        String resetToken = java.util.UUID.randomUUID().toString().replace("-", "");

        // 4. 存入 Redis，有效期 3 分钟
        String key = RedisEnum.RESET_PASSWORD_TOKEN.getKey() + email;
        redisClient.set(key, resetToken, 180);

        return resetToken;
    }
}
