package com.xigua.client.config;

import com.xigua.client.ws.WsHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * @ClassName WebSocketConfig
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/8 18:10
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    /**
     * WebSocket 连接路径
     * /connect/{userId}
     */
    public static final String WS_CONNECT_PATH = "/connect/{userId}";

    @Autowired
    private WsHandler wsHandler;

    @Autowired
    private AuthHandshakeInterceptor authHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(wsHandler, WS_CONNECT_PATH)
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

    /**
     * 放宽 WebSocket 文本帧上限。
     * 业务允许单条聊天消息 20000 字符，中文 UTF-8 编码后约 60KB；
     * Tomcat 默认 8KB 缓冲会直接以 1009 关闭连接，导致长消息一发就断线。
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(128 * 1024);
        container.setMaxBinaryMessageBufferSize(128 * 1024);
        return container;
    }
}
