package com.xigua.common.core.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.xigua.common.core.model.UserToken;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * @ClassName TokenUtil
 * @Description
 * @Author wangjinfei
 * @Date 2025/3/25 16:56
 */
@Slf4j
public class TokenUtil {
    //密钥盐
    public static final String TOKEN_SECRET="xigua";
    // 与登录接口 Redis 会话 TTL 保持一致，避免 JWT 无期限有效
    public static final long TOKEN_TTL_SECONDS = 3 * 60 * 60;

    public static String genToken(UserToken userToken) {
        String token = null;

        try {
            Date issuedAt = new Date();
            token = JWT.create()
                    .withClaim("userName", userToken.getUserName())
                    .withClaim("userId", userToken.getUserId())
                    .withClaim("phone", userToken.getPhone())
                    .withIssuedAt(issuedAt)
                    .withJWTId(java.util.UUID.randomUUID().toString())
                    .withExpiresAt(new Date(issuedAt.getTime() + TOKEN_TTL_SECONDS * 1000))
                    // 使用了HMAC256加密算法
                    .sign(Algorithm.HMAC256(TOKEN_SECRET));
        } catch (Exception e){
            e.printStackTrace();
        }
        return token;
    }

    /**
     * 设备级 token 会话 key：同一用户每台设备一条，互不覆盖。
     * 只存 token 的 SHA-256 摘要，避免把完整凭证写进 Redis key。
     */
    public static String deviceTokenKey(String userId, String token) {
        return "TOKEN:" + userId + ":device:" + sha256(token);
    }

    public static String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
    /**
     * 判断token是否过期
     * @author wangjinfei
     * @date 2025/5/11 21:30
     * @param token
     * @return Boolean
    */
    public static Boolean tokenExpire(String token) {
        try {
            JWT.require(Algorithm.HMAC256(TOKEN_SECRET)).build().verify(token);
            return false;
        } catch (TokenExpiredException e){
            log.error("token过期：", e);
            return true;
        } catch (JWTVerificationException e){
            log.error("token无效：", e);
            return true;
        }
    }
}
