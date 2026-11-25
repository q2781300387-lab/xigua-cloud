package com.xigua.api.interceptor;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.core.util.TokenUtil;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.common.core.model.UserToken;
import com.xigua.domain.enums.RedisEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * @ClassName LoginInterceptor
 * @Description
 * @Author wangjinfei
 * @Date 2025/3/28 15:45
 */
@Component
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    private static final String TRACE_ID = "traceId";
    private static final String TOKEN_HEADER = "xigua-token";
    private static final String USER_AGENT = "User-Agent";
    private static final String CONTENT_TYPE = "application/json;charset=UTF-8";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private Sequence sequence;
    @Autowired
    private RedisClient redisClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 生成链路id 存储到mdc
        String traceId = sequence.nextNo();
        MDC.put(TRACE_ID, traceId);

        // 认证入口
        return doAuth(request, response);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex){
        MDC.remove(TRACE_ID);
        UserContext.clear();
    }

    private boolean doAuth(HttpServletRequest request, HttpServletResponse response) {
        String token = request.getHeader(TOKEN_HEADER);
        String ua = request.getHeader(USER_AGENT);
        if (StringUtils.isBlank(token)) {
            return sendUnauthorized(response, 401, "未提供认证令牌");
        }

        // 验证JWT签名和格式
        DecodedJWT decodedJWT;
        try {
            decodedJWT = JWT.require(Algorithm.HMAC256(TokenUtil.TOKEN_SECRET))
                    .build()
                    .verify(token);
        } catch (JWTVerificationException e) {
            log.warn("Token验证失败: token={}, uri={}, error={}",
                    token, request.getRequestURI(), e.getMessage());
            return sendUnauthorized(response, 401, "认证无效");
        }

        // 历史 token 没有 exp，不得继续作为长期凭证使用
        if (decodedJWT.getExpiresAt() == null) {
            return sendUnauthorized(response, 401, "令牌缺少有效期");
        }

        // 提取用户信息
        String userId = decodedJWT.getClaim("userId").asString();
        if (StringUtils.isBlank(userId)) {
            return sendUnauthorized(response, 401, "令牌信息不完整");
        }

        // 检查 Redis 中的会话状态：优先校验设备级 key，再回退旧版用户级 key。
        // 旧逻辑只看 key 是否存在，导致同用户旧 token 在 TTL 内一直可用；这里必须比对 token 值。
        String redisKey = RedisEnum.TOKEN.getKey() + userId;
        String deviceKey = TokenUtil.deviceTokenKey(userId, token);

        Long deviceExpire = redisClient.getExpire(deviceKey);
        if (deviceExpire == null) {
            log.error("Redis连接异常，无法验证会话状态: userId={}", userId);
            return sendError(response, 503, "系统繁忙，请稍后重试");
        }

        boolean valid = false;
        if (deviceExpire > 0) {
            String saved = redisClient.get(deviceKey);
            valid = token.equals(saved);
        } else {
            // 兼容旧 key（后台端与登录改造前签发的 token）
            Long expireSeconds = redisClient.getExpire(redisKey);
            if (expireSeconds == null) {
                log.error("Redis连接异常，无法验证会话状态: userId={}", userId);
                return sendError(response, 503, "系统繁忙，请稍后重试");
            }
            if (expireSeconds > 0) {
                String saved = redisClient.get(redisKey);
                valid = token.equals(saved);
            } else {
                log.info("会话不存在或已过期: userId={}, uri={}", userId, request.getRequestURI());
            }
        }

        if (!valid) {
            return sendUnauthorized(response, 401, "认证已过期，请重新登录");
        }

        // 封禁账号即使还留着 token 也必须挡住，避免封禁前签发的旧凭证继续可用
        String userCache = redisClient.get(RedisEnum.USER.getKey() + userId);
        if (StringUtils.isNotBlank(userCache)) {
            try {
                JsonNode statusNode = OBJECT_MAPPER.readTree(userCache).get("status");
                if (statusNode != null && statusNode.asInt() == 2) {
                    log.info("封禁账号访问被拦截: userId={}, uri={}", userId, request.getRequestURI());
                    return sendUnauthorized(response, 401, "账号已被封禁");
                }
            } catch (Exception e) {
                log.warn("解析用户缓存失败，放行本次请求: userId={}, error={}", userId, e.getMessage());
            }
        }

        // 构建用户上下文
        UserToken userToken = UserToken.builder()
                .userId(userId)
                .userName(decodedJWT.getClaim("userName").asString())
                .phone(decodedJWT.getClaim("phone").asString())
                .userAgent(ua)
                .build();

        UserContext.set(userToken);

//        // 刷新Token有效期（续期）
//        if (expireSeconds < TokenUtil.REFRESH_THRESHOLD) {
//            redisClient.expire(redisKey, TokenUtil.TOKEN_EXPIRE_HOURS * 3600);
//            log.debug("Token续期: userId={}, newExpire={}h",
//                    userId, TokenUtil.TOKEN_EXPIRE_HOURS);
//        }

        return true;
    }

    /**
     * 发送401未授权响应（统一处理）
     */
    private boolean sendUnauthorized(HttpServletResponse response, int code, String msg) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(CONTENT_TYPE);
            response.getWriter().write(String.format("{\"code\":%d,\"msg\":\"%s\"}", code, msg));
            return false;
        } catch (IOException e) {
            log.error("返回401响应异常: msg={}", msg, e);
            return false;
        }
    }

    /**
     * 发送其他错误响应
     */
    private boolean sendError(HttpServletResponse response, int code, String msg) {
        try {
            response.setStatus(code);
            response.setContentType(CONTENT_TYPE);
            response.getWriter().write(String.format("{\"code\":%d,\"msg\":\"%s\"}", code, msg));
            return false;
        } catch (IOException e) {
            log.error("返回{}响应异常: msg={}", code, msg, e);
            return false;
        }
    }
}
