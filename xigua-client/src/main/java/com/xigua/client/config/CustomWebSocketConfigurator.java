package com.xigua.client.config;

import com.xigua.common.core.util.NetUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.server.WsHandshakeRequest;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;

/**
 * @ClassName CustomWebSocketConfig
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/10/26 10:59
 */
@Slf4j
@Configuration
public class CustomWebSocketConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig sec,HandshakeRequest request,HandshakeResponse response) {
        String clientIp = "unknown";

        // ws是通过http升级而来的，所以可以从http request中获取ip

        if (request instanceof WsHandshakeRequest) {
            try {
                // 反射获取HttpServletRequest
                Field field = request.getClass().getDeclaredField("request");
                field.setAccessible(true);
                HttpServletRequest httpServletRequest = (HttpServletRequest) field.get(request);
                clientIp = NetUtil.getClientIp(httpServletRequest);
            } catch (Exception e) {
                log.error("获取WS session失败", e);
            }
        }
        sec.getUserProperties().put("clientIp", clientIp);
    }
}
