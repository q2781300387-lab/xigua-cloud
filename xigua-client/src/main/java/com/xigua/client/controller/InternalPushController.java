package com.xigua.client.controller;

import com.xigua.api.service.ClientService;
import com.xigua.common.core.base.R;
import com.xigua.domain.ws.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 中台到客户端节点的内部推送入口。
 *
 * <p>原实现让中台在请求线程里动态创建 Dubbo ReferenceConfig 直连节点，冷启动时会阻塞在
 * 服务发现上，导致视频来电、聊天消息都无法下发。这里改为中台通过本机 HTTP 直连推送，
 * 客户端节点只负责把消息写入当前在线的 WebSocket 会话。</p>
 *
 * <p>该接口仅供内网中台调用，使用共享令牌校验，避免被公网请求当成任意消息注入入口。</p>
 */
@Slf4j
@RestController
@RequestMapping("/internal/push")
public class InternalPushController {

    private static final String DEFAULT_PUSH_TOKEN = "xigua-internal-push-2026";

    @Autowired
    private ClientService clientService;

    @Value("${xigua.internal.push-token:" + DEFAULT_PUSH_TOKEN + "}")
    private String pushToken;

    @PostMapping
    public R<Boolean> push(
            @RequestHeader(value = "X-Internal-Push-Token", required = false) String token,
            @RequestBody MessageResponse messageResponse) {
        if (!Objects.equals(pushToken, token)) {
            log.warn("拒绝非法内部推送请求：receiverId={}", messageResponse == null ? null : messageResponse.getReceiverId());
            return R.fail("无权调用内部推送接口");
        }
        if (messageResponse == null || messageResponse.getReceiverId() == null) {
            return R.fail("缺少接收人");
        }
        clientService.receiveMessage4Center(messageResponse);
        return R.ok(true);
    }
}
