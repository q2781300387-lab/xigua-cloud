package com.xigua.common.core.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * 密码哈希工具。
 *
 * <p>新密码一律使用 BCrypt 存储（自带随机盐、可调强度，抗彩虹表与暴力破解）。
 * 历史数据是无盐 MD5，因此 {@link #matches(String, String)} 兼容旧哈希：
 * 命中旧格式时按 MD5 比对，供登录流程识别并顺带升级为 BCrypt。</p>
 */
public final class PasswordUtil {

    /** BCrypt 强度，默认 10（约 2^10 轮），兼顾安全与单次登录耗时 */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /** 历史无盐 MD5：固定 32 位十六进制 */
    private static final Pattern LEGACY_MD5 = Pattern.compile("^[a-fA-F0-9]{32}$");

    private PasswordUtil() {
    }

    /**
     * 生成 BCrypt 哈希，形如 $2a$10$xxxx
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("password must not be empty");
        }
        return ENCODER.encode(rawPassword);
    }

    /**
     * 判断存储值是否为历史 MD5 哈希（用于登录时决定是否升级）
     */
    public static boolean isLegacyHash(String storedHash) {
        return storedHash != null && LEGACY_MD5.matcher(storedHash).matches();
    }

    /**
     * 校验明文密码是否匹配存储值，自动兼容 BCrypt 与历史 MD5
     */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || rawPassword.isEmpty() || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        if (isLegacyHash(storedHash)) {
            return storedHash.equalsIgnoreCase(md5Hex(rawPassword));
        }
        try {
            return ENCODER.matches(rawPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // 存储值既不是 MD5 也不是合法 BCrypt（历史脏数据），按校验失败处理
            return false;
        }
    }

    /**
     * 历史 MD5 计算，仅用于兼容比对旧数据
     */
    public static String md5Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }
}
