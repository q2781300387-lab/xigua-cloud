package com.xigua.admin.service;

import com.xigua.api.service.admin.CaptchaService;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.vo.admin.CaptchaImageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

/**
 * @ClassName CaptchaServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/21 21:27
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final long EXPIRE_SECONDS = 120;

    @Autowired
    private RedisClient redisClient;

    @Override
    public CaptchaImageVO image() {
        // 是否启用（可走配置）
        boolean isEnabled = true;
        if (!isEnabled) {
            return buildDisabledCaptcha();
        }

        // 1. 生成验证码文本
        String code = generateCode(CODE_LENGTH);

        // 2. 生成 uuid
        String uuid = UUID.randomUUID().toString();

        // 3. 生成图片
        BufferedImage image = generateImage(code);

        // 4. 图片转 base64
        String base64 = imageToBase64(image);

        // 5. 存 redis
        String redisKey = RedisEnum.CAPTCHA.getKey() + uuid;
        redisClient.set(redisKey, code, EXPIRE_SECONDS);

        // 6. 返回
        CaptchaImageVO vo = new CaptchaImageVO();
        vo.setUuid(uuid);
        vo.setImg(base64);
        vo.setEnabled(true);
        vo.setExpireTime(System.currentTimeMillis() + EXPIRE_SECONDS * 1000);

        return vo;
    }

    private CaptchaImageVO buildDisabledCaptcha() {
        CaptchaImageVO vo = new CaptchaImageVO();
        vo.setEnabled(false);
        vo.setExpireTime(0);
        vo.setImg(null);
        vo.setUuid(null);
        return vo;
    }

    private String generateCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private BufferedImage generateImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
            g.drawLine(
                    random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT)
            );
        }

        // 字体
        g.setFont(new Font("Arial", Font.BOLD, 24));

        // 文字
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            g.drawString(
                    String.valueOf(code.charAt(i)),
                    20 + i * 22,
                    28
            );
        }

        g.dispose();
        return image;
    }

    private String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            return "data:image/png;base64," + base64;
        } catch (IOException e) {
            throw new RuntimeException("生成验证码失败", e);
        }
    }
}
