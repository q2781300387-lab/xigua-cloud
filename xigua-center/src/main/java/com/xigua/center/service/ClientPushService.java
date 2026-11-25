package com.xigua.center.service;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.domain.connect.Client;
import com.xigua.domain.ws.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 中台向客户端节点下发消息的 HTTP 通道。
 *
 * <p>不再在业务线程里创建 Dubbo ReferenceConfig。动态引用在冷启动阶段会阻塞在注册中心
 * 服务发现上，视频呼叫表现为“中台收到请求但对方永远收不到来电”。HTTP 直连只做一件事：
 * 把已经构造好的消息交给目标节点的 WebSocket 会话。</p>
 */
@Slf4j
@Component
public class ClientPushService {

    private static final String PUSH_TOKEN = "xigua-internal-push-2026";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public void push(Client node, MessageResponse messageResponse) {
        if (node == null || node.getHost() == null || node.getPort() == null) {
            throw new IllegalArgumentException("客户端节点信息不完整");
        }

        String url = "http://" + node.getHost() + ":" + node.getPort() + "/internal/push";
        String body = JSONObject.toJSONString(messageResponse);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("X-Internal-Push-Token", PUSH_TOKEN)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("推送失败，HTTP " + response.statusCode() + "：" + response.body());
            }
            log.debug("推送消息到节点 {}:{} 成功，receiverId={}，subType={}",
                    node.getHost(), node.getPort(), messageResponse.getReceiverId(), messageResponse.getSubType());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("推送被中断：" + url, e);
        } catch (Exception e) {
            log.error("推送消息到节点 {}:{} 失败，receiverId={}，subType={}",
                    node.getHost(), node.getPort(), messageResponse.getReceiverId(), messageResponse.getSubType(), e);
            throw new IllegalStateException("推送消息到客户端节点失败：" + e.getMessage(), e);
        }
    }
}
