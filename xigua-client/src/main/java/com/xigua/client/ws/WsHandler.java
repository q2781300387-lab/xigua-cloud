package com.xigua.client.ws;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.ClientService;
import com.xigua.client.helper.WsSessionHelper;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.ws.MessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * @ClassName WsHandler
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/8 18:12
 */
@Slf4j
@Component
public class WsHandler extends TextWebSocketHandler {

    @Autowired
    private ClientService clientService;

    @Autowired
    private RedisClient redisClient;

    /** 连接建立（原 @OnOpen） */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        String clientIp = (String) session.getAttributes().get("clientIp");
        String userCache = (String) session.getAttributes().get("userCache");

        WsSessionHelper.put(userId, session);
        log.info("---->>>>>[websocket] 新的连接：clientIp={}，userId={}", clientIp, userId);

        WsSessionHelper.buildToken(JSONObject.parseObject(userCache, User.class));
        try {
            clientService.clientRegister2Center(userId);
        } catch (Exception e) {
            log.error("---->>>>>[websocket] 注册用户到长连接服务器系统异常：", e);
        } finally {
            WsSessionHelper.unBuildToken();
        }
    }

    /** 收到消息（原 @OnMessage） */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userId = (String) session.getAttributes().get("userId");
        String clientIp = (String) session.getAttributes().get("clientIp");

        log.info("---->>>>>[websocket] 收到消息：sessionId={}，clientIp={}，message={}",
                session.getId(), clientIp, message.getPayload());

        MessageRequest messageRequest =
                JSONObject.parseObject(message.getPayload(), MessageRequest.class);

        // 发送人身份必须由服务端握手态决定，客户端上报的 senderId 一律不可信。
        // 否则任意登录用户都能伪造别人的 senderId 往会话里发消息。
        String subType = messageRequest.getSubType();
        boolean botSelfPersist = ("bot_response_end".equals(subType) || "stop_bot_response".equals(subType))
                && userId.equals(messageRequest.getReceiverId());
        // bot 的收尾/暂停帧需要保留机器人 senderId 来落库，但只允许写进自己的机器人会话；
        // 其余所有帧一律强制成当前登录用户，避免伪造成他人发消息。
        if (!botSelfPersist) {
            messageRequest.setSenderId(userId);
        }
        messageRequest.setServerReceiveTime(System.currentTimeMillis());
        messageRequest.setIp(clientIp);

        try {
            // todo 可以优化成异步发送
            clientService.sendMessage2Center(messageRequest);
        } catch (BusinessException e) {
            log.error("---->>>>>[websocket] 发送消息到长连接服务器业务异常：", e);
        } catch (Exception e) {
            log.error("---->>>>>[websocket] 发送消息到长连接服务器系统异常：", e);
        }
    }

    /** 连接关闭（原 @OnClose） */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");

        // 多端同步：只移除当前这一路连接
        boolean lastSession = WsSessionHelper.remove(userId, session.getId());
        log.info("---->>>>>[websocket] 连接断开：userId={}，reason={}，剩余在线连接={}",
                userId, status, lastSession ? 0 : 1);

        if (!lastSession) {
            // 该用户还有其他端在线，仅移除本地连接，不注销长连接服务器
            return;
        }

        String userCache = redisClient.get(RedisEnum.USER.getKey() + userId);
        if (StringUtils.isEmpty(userCache)) {
            return;
        }

        clientService.clientDeregister2Center(userId);
    }

    /** 连接异常（原 @OnError） */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("---->>>>>[websocket] 连接异常：", exception);
    }
}
