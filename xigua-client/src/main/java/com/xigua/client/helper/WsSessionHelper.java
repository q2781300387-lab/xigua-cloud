package com.xigua.client.helper;

import com.xigua.common.core.model.UserToken;
import com.xigua.common.core.util.UserContext;
import com.xigua.domain.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName SessionHelper
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/4/20 19:41
 */
public class WsSessionHelper {
    /**
     * 多端在线连接集合：userId -> sessionId -> session
     * 同一账号可在多个设备/标签页同时在线，互不覆盖
     */
    private static Map<String, ConcurrentHashMap<String, WebSocketSession>> wsSessionMap = new ConcurrentHashMap<>();

    public static void put(String userId, WebSocketSession session) {
        ConcurrentHashMap<String, WebSocketSession> sessions =
                wsSessionMap.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        sessions.put(session.getId(), session);
    }

    /** 获取该用户的全部在线连接（多端） */
    public static Collection<WebSocketSession> getSessions(String userId) {
        ConcurrentHashMap<String, WebSocketSession> sessions = wsSessionMap.get(userId);
        if (sessions == null) {
            return Collections.emptyList();
        }
        return sessions.values();
    }

    /** 获取该用户任一在线连接（注册/登录信息用，各端信息一致） */
    public static WebSocketSession get(String userId) {
        Collection<WebSocketSession> sessions = getSessions(userId);
        if (sessions.isEmpty()) {
            return null;
        }
        return sessions.iterator().next();
    }

    /**
     * 移除指定连接
     * @return true 表示该用户已无剩余在线连接（最后一路断开）
     */
    public static boolean remove(String userId, String sessionId) {
        ConcurrentHashMap<String, WebSocketSession> sessions = wsSessionMap.get(userId);
        if (sessions == null) {
            return true;
        }
        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            wsSessionMap.remove(userId);
            return true;
        }
        return false;
    }

    public static void buildToken(User user){
        // 生成token
        UserToken userToken = UserToken.builder().build();
        userToken.setUserName(StringUtils.isNotEmpty(user.getUsername()) ? user.getUsername() : "");
        userToken.setUserId(StringUtils.isNotEmpty(user.getId()) ? user.getId() : "");
        userToken.setPhone(StringUtils.isNotEmpty(user.getPhone()) ? user.getPhone() : "");

        // userContext 赋值
        UserContext.set(userToken);
    }

    public static void unBuildToken(){
        UserContext.clear();
    }
}
