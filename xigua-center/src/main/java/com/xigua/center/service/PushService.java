package com.xigua.center.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigua.center.entity.PushSubscription;
import com.xigua.center.mapper.PushSubscriptionMapper;
import com.xigua.center.util.WebPushSender;
import com.xigua.domain.ws.MessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName PushService
 * @Description 离线兜底推送：接收人没有活跃 WS 连接时，用 Web Push 把消息送到浏览器
 * @Author xigua
 * @Date 2026/11/12 19:40
 */
@Slf4j
@Service
public class PushService {

    @Autowired
    private PushSubscriptionMapper pushSubscriptionMapper;

    @Value("${xigua.push.vapid-public-key:}")
    private String vapidPublicKey;

    @Value("${xigua.push.vapid-private-key:}")
    private String vapidPrivateKey;

    @Value("${xigua.push.subject:mailto:admin@xgchat.cn}")
    private String vapidSubject;

    /**
     * 对某条单聊消息做离线推送
     */
    public void pushChatMessage(MessageRequest request) {
        pushChatMessage(request, request.getReceiverId(),
                String.valueOf(request.getSenderId()), resolveTitle(request), request.getChatType());
    }

    /**
     * 对群聊消息做离线推送（由群发链路逐个离线成员调用）
     */
    public void pushGroupChatMessage(MessageRequest request, String receiverId, String groupName) {
        pushChatMessage(request, receiverId, String.valueOf(request.getReceiverId()),
                isBlank(groupName) ? "群聊新消息" : groupName, 2);
    }

    private void pushChatMessage(MessageRequest request, String receiverIdValue, String chatId,
                                 String title, Integer chatType) {
        if (isBlank(vapidPublicKey) || isBlank(vapidPrivateKey)) {
            log.debug("----->>>>>> 未配置 VAPID，跳过 Web Push");
            return;
        }
        Long receiverId;
        try {
            receiverId = Long.valueOf(receiverIdValue);
        } catch (Exception e) {
            return;
        }

        List<PushSubscription> subs = pushSubscriptionMapper.selectList(
                new LambdaQueryWrapper<PushSubscription>()
                        .eq(PushSubscription::getUserId, receiverId));

        if (subs == null || subs.isEmpty()) {
            return;
        }

        JSONObject payload = new JSONObject();
        payload.put("title", title);
        payload.put("body", buildBody(request));
        payload.put("chatId", chatId);
        payload.put("chatType", chatType);
        payload.put("tag", "xg-" + chatId);
        payload.put("url", "/chat");

        WebPushSender sender = new WebPushSender(vapidPublicKey, vapidPrivateKey, vapidSubject);
        int ok = 0;
        for (PushSubscription sub : subs) {
            boolean sent = sender.send(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), payload);
            if (sent) {
                ok++;
            } else {
                // 端点失效（410/404）：清掉，避免反复失败
                pushSubscriptionMapper.deleteById(sub.getId());
            }
        }
        log.info("----->>>>>> Web Push 离线推送完成：receiverId={}，订阅数={}，成功={}",
                receiverId, subs.size(), ok);
    }

    private String resolveTitle(MessageRequest request) {
        if (request.getChatType() != null && request.getChatType() == 2) {
            return "群聊新消息";
        }
        return "新消息";
    }

    private String buildBody(MessageRequest request) {
        Object content = request.getMessage();
        String text = content == null ? "" : String.valueOf(content);
        if (text.length() > 60) {
            text = text.substring(0, 60) + "...";
        }
        return text.isEmpty() ? "你有一条新消息" : text;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
