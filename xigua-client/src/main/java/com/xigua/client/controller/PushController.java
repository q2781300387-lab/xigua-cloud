package com.xigua.client.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigua.client.entity.PushSubscription;
import com.xigua.client.mapper.PushSubscriptionMapper;
import com.xigua.common.core.base.R;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.sequence.sequence.Sequence;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @ClassName PushController
 * @Description Web Push 订阅登记：浏览器拿到 endpoint/密钥后上报，服务端离线时据此下发系统通知
 * @Author xigua
 * @Date 2026/11/12 19:20
 */
@Slf4j
@Tag(name = "Web Push 订阅接口")
@RequestMapping("/push")
@RestController
public class PushController {

    @Autowired
    private PushSubscriptionMapper pushSubscriptionMapper;

    @Autowired
    private Sequence sequence;

    @Value("${xigua.push.vapid-public-key:}")
    private String vapidPublicKey;

    /**
     * 前端订阅前先取 VAPID 公钥（applicationServerKey）
     */
    @Operation(summary = "获取 VAPID 公钥")
    @GetMapping("/publicKey")
    public R<String> publicKey() {
        if (vapidPublicKey == null || vapidPublicKey.trim().isEmpty()) {
            return R.fail("未配置 VAPID 公钥");
        }
        return R.ok(vapidPublicKey.trim());
    }

    /**
     * 登记订阅：同一 endpoint 幂等复用，同一用户可多设备
     */
    @Operation(summary = "登记 Web Push 订阅")
    @PostMapping("/subscribe")
    public R<Boolean> subscribe(@RequestBody Map<String, String> body) {
        String userId = UserContext.get().getUserId();
        String endpoint = body.get("endpoint");
        String p256dh = body.get("p256dh");
        String auth = body.get("auth");
        if (endpoint == null || endpoint.trim().isEmpty()
                || p256dh == null || p256dh.trim().isEmpty()
                || auth == null || auth.trim().isEmpty()) {
            return R.fail("订阅参数不完整");
        }
        if (userId == null) {
            return R.fail("未登录");
        }

        PushSubscription exist = pushSubscriptionMapper.selectOne(
                new LambdaQueryWrapper<PushSubscription>()
                        .eq(PushSubscription::getEndpoint, endpoint)
                        .last("limit 1"));

        if (exist != null) {
            exist.setUserId(Long.valueOf(userId));
            exist.setP256dh(p256dh);
            exist.setAuth(auth);
            exist.setUa(body.get("ua"));
            exist.setUpdateTime(LocalDateTime.now());
            pushSubscriptionMapper.updateById(exist);
            log.info("----->>>>>> Web Push 订阅已更新：userId={}，endpoint={}...",
                    userId, endpoint.length() > 60 ? endpoint.substring(0, 60) : endpoint);
            return R.ok(true);
        }

        PushSubscription sub = new PushSubscription();
        sub.setId(Long.valueOf(sequence.nextNo()));
        sub.setUserId(Long.valueOf(userId));
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        sub.setUa(body.get("ua"));
        sub.setDelFlag(0);
        sub.setCreateTime(LocalDateTime.now());
        pushSubscriptionMapper.insert(sub);
        log.info("----->>>>>> Web Push 订阅已登记：userId={}，endpoint={}...",
                userId, endpoint.length() > 60 ? endpoint.substring(0, 60) : endpoint);
        return R.ok(true);
    }

    /**
     * 注销订阅（用户关闭通知或退出登录时）
     */
    @Operation(summary = "注销 Web Push 订阅")
    @PostMapping("/unsubscribe")
    public R<Boolean> unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint == null || endpoint.trim().isEmpty()) {
            return R.fail("缺少 endpoint");
        }
        PushSubscription exist = pushSubscriptionMapper.selectOne(
                new LambdaQueryWrapper<PushSubscription>()
                        .eq(PushSubscription::getEndpoint, endpoint)
                        .last("limit 1"));
        if (exist == null) {
            return R.ok(true);
        }
        pushSubscriptionMapper.deleteById(exist.getId());
        return R.ok(true);
    }
}
