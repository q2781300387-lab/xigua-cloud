package com.xigua.client.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.alibaba.fastjson2.JSONObject;
import com.xigua.common.core.util.NetUtil;
import com.xigua.common.core.util.TokenUtil;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.RedisEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * @ClassName AuthHandshakeInterceptor
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/8 18:11
 */
@Slf4j
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private RedisClient redisClient;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String path = request.getURI().getPath(); // /client/connect/{userId}
        String userId = path.substring(path.lastIndexOf("/") + 1);

        String clientIp = "unknown";
        String ua = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            clientIp = NetUtil.getClientIp(httpServletRequest);
            ua = httpServletRequest.getHeader("User-Agent");
        }

        // 握手必须携带登录 token：浏览器 WebSocket 不能自定义 Header，所以支持 ?token= 查询参数，
        // 同时兼容非浏览器客户端用 xigua-token 头。只校验 URL 里的 userId 会把任意用户 ID 变成通行证。
        String token = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("token");
        if (StringUtils.isEmpty(token)) {
            token = request.getHeaders().getFirst("xigua-token");
        }
        if (StringUtils.isEmpty(token)) {
            log.info("---->>>>>[websocket] 非法连接：缺少 token，ip={}，userId={}", clientIp, userId);
            return false;
        }
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(TokenUtil.TOKEN_SECRET))
                    .build()
                    .verify(token);
            if (decodedJWT.getExpiresAt() == null) {
                log.info("---->>>>>[websocket] 非法连接：token 缺少有效期，ip={}，userId={}", clientIp, userId);
                return false;
            }
            String tokenUserId = decodedJWT.getClaim("userId").asString();
            if (StringUtils.isEmpty(tokenUserId) || !tokenUserId.equals(userId)) {
                log.info("---->>>>>[websocket] 非法连接：token 身份不匹配，ip={}，pathUserId={}，tokenUserId={}",
                        clientIp, userId, tokenUserId);
                return false;
            }
        } catch (Exception e) {
            log.info("---->>>>>[websocket] 非法连接：token 校验失败，ip={}，userId={}，reason={}",
                    clientIp, userId, e.getMessage());
            return false;
        }

        String userCache = redisClient.get(RedisEnum.USER.getKey() + userId);
        if (StringUtils.isEmpty(userCache)) {
            log.info("---->>>>>[websocket] 非法连接：ip={}，userId={}", clientIp, userId);
            return false;
        }

        // 握手也必须校验 Redis 中的 token 值，不能只验 JWT 签名
        String deviceKey = TokenUtil.deviceTokenKey(userId, token);
        String deviceSaved = redisClient.get(deviceKey);
        if (StringUtils.isNotEmpty(deviceSaved)) {
            if (!token.equals(deviceSaved)) {
                log.info("---->>>>>[websocket] 非法连接：token 已失效，ip={}，userId={}", clientIp, userId);
                return false;
            }
        } else {
            String legacySaved = redisClient.get(RedisEnum.TOKEN.getKey() + userId);
            if (StringUtils.isEmpty(legacySaved) || !token.equals(legacySaved)) {
                log.info("---->>>>>[websocket] 非法连接：会话不存在或 token 不匹配，ip={}，userId={}", clientIp, userId);
                return false;
            }
        }

        User user = JSONObject.parseObject(userCache, User.class);
        if (user != null && Integer.valueOf(2).equals(user.getStatus())) {
            log.info("---->>>>>[websocket] 账号已封禁，拒绝连接：ip={}，userId={}", clientIp, userId);
            return false;
        }

        // 放到 session attributes，后面直接用
        attributes.put("userId", userId);
        attributes.put("clientIp", clientIp);
        attributes.put("ua", ua);
        attributes.put("userCache", userCache);

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
    }
}
