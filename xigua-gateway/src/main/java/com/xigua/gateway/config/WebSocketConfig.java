package com.xigua.gateway.config;

import jakarta.servlet.ServletContext;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerContainer;
import org.apache.catalina.Context;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.client.TomcatWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.upgrade.TomcatRequestUpgradeStrategy;

/**
 * @ClassName WebSocketConfig
 * @Description websocket通过网关代理的配置
 * @Author wangjinfei
 * @Date 2025/1/23 11:50
 */
@Component
public class WebSocketConfig {
    /**
     * class org.apache.catalina.connector.ResponseFacade cannot be cast to class reactor.netty.http.server.HttpServerResponse
     * 网关默认使用是基于 Netty 容器的 RequestUpgradeStrategy 和 WebSocketClient 导致错误，声明 Tomcat 容器对应的 Bean 来覆盖它就可以解决这个问题
    */

    private static final int MAX_TEXT_MESSAGE_BUFFER_SIZE = 128 * 1024;

    @Bean
    @Primary
    public WebSocketClient tomcatWebSocketClient() {
        // 使用tomcat的websocket客户端，并放宽与下游 client 的帧上限；
        // 默认 8KB 会让 20000 字符消息在网关侧以 1009 断开。
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(MAX_TEXT_MESSAGE_BUFFER_SIZE);
        container.setDefaultMaxBinaryMessageBufferSize(MAX_TEXT_MESSAGE_BUFFER_SIZE);
        return new TomcatWebSocketClient(container);
    }

    /**
     * 浏览器 -> 网关这一侧的 WebSocket 文本帧上限。
     * 网关是 reactive 模型启动的 Tomcat，拿不到 ServletWebServerApplicationContext，
     * 因此从 WebServerInitializedEvent 取 TomcatWebServer，再设置 WsServerContainer。
     */
    @EventListener
    public void onWebServerReady(WebServerInitializedEvent event) {
        if (event.getApplicationContext() instanceof ServletWebServerApplicationContext ctx) {
            applyBufferSize(ctx.getServletContext());
            return;
        }
        if (event.getWebServer() instanceof TomcatWebServer tomcatWebServer) {
            Context ctx = (Context) tomcatWebServer.getTomcat().getHost().findChild("");
            if (ctx != null && ctx.getServletContext() != null) {
                applyBufferSize(ctx.getServletContext());
            }
        }
    }

    private void applyBufferSize(ServletContext servletContext) {
        Object attr = servletContext.getAttribute("jakarta.websocket.server.ServerContainer");
        if (attr instanceof ServerContainer serverContainer) {
            serverContainer.setDefaultMaxTextMessageBufferSize(MAX_TEXT_MESSAGE_BUFFER_SIZE);
            serverContainer.setDefaultMaxBinaryMessageBufferSize(MAX_TEXT_MESSAGE_BUFFER_SIZE);
        }
    }

    @Bean
    @Primary
    public RequestUpgradeStrategy requestUpgradeStrategy() {
        // 使用tomcat请求响应策略
        return new TomcatRequestUpgradeStrategy();
    }
}
