package com.xigua.center.service;

import com.xigua.center.helper.ClientNode;
import com.xigua.center.helper.ClientNodeHelper;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.connect.Client;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.api.service.CenterService;
import com.xigua.api.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @ClassName TimerTaskServiceImpl
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/6/16 20:37
 */
@Slf4j
@Service
@DubboService
public class JobServiceImpl implements JobService {
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private CenterService centerService;

    @Override
    public void checkOnlineConnection() {
        /**
         * todo 如果在线用户过多
         * 可以采用分片的方式查询reids中的在线用户
         * 负载均衡的方式执行任务方式
        */

        // 获取所有的节点信息
        Set<String> wsClients = ClientNodeHelper.getAllClientIds();

        // 节点信息为空，删除所有在线用户
        if(CollectionUtils.isEmpty(wsClients)){
            redisClient.delAll(RedisEnum.ONLINE_USER.getKey() + "*");
            redisClient.delAll(RedisEnum.ONLINE_NODE_USER.getKey() + "*");
            return;
        }

        // 根据wsClients创建异步任务 检查在线用户
        int total = wsClients.size();
        log.info("开始检查{}个客户端节点的在线用户", total);

        // 使用CompletableFuture管理所有任务
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String client : wsClients) {
            ClientNode clientNode = ClientNodeHelper.get(client);
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> checkSingleClient(clientNode.getIp()));
            futures.add(future);
        }

        try {
            // 等待所有任务完成，设置超时时间
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            allFutures.get(30, TimeUnit.SECONDS); // 最多等待30秒
            log.info("在线用户检查完成，共检查{}个客户端节点", total);

        } catch (TimeoutException e) {
            log.warn("在线用户检查超时，部分任务可能未完成");
        } catch (Exception e) {
            log.error("在线用户检查异常", e);
        }
    }

    private void checkSingleClient(String clientIp) {
        // 获取指定节点在线用户
        Set<Object> onlineUsers = redisClient.members(RedisEnum.ONLINE_NODE_USER.getKey() + clientIp);
        if(CollectionUtils.isEmpty(onlineUsers)){
            log.info("{}客户端节点没有在线用户", clientIp);
            return;
        }

        for (Object onlineUser : onlineUsers) {
            // 获取在线用户的最后心跳时间
            String lastPing = redisClient.get(RedisEnum.LAST_PING_TIME.getKey() + onlineUser);
            if(StringUtils.isEmpty(lastPing)){
                // 新连接还没发首个 ping 时不能直接判定离线，否则会在注册后的一个巡检周期内被误删
                Client client = centerService.onlineUser(String.valueOf(onlineUser));
                Long loginTime = client == null ? null : client.getLoginTime();
                if (loginTime != null && System.currentTimeMillis() - loginTime < 150000) {
                    continue;
                }
                // 如果没有最后心跳时间且连接已超过阈值，说明用户不在线 删除在线用户
                delOnlineUser(clientIp, String.valueOf(onlineUser));
                continue;
            }

            long timestamp = System.currentTimeMillis();
            if(timestamp - Long.parseLong(lastPing) > 150000){
                // 如果最后心跳时间超过150秒，说明用户不在线 删除在线用户
                delOnlineUser(clientIp, String.valueOf(onlineUser));
                continue;
            }
        }
    }

    /**
     * redis 删除在线用户
     * @author wangjinfei
     * @date 2025/6/16 21:11
     * @param onlineUser
    */
    private void delOnlineUser(String clientId, String onlineUser) {
        // 用户不在线
        log.info("----->>>>>> 用户{}不在线", onlineUser);
        Client client = centerService.onlineUser(onlineUser);
        if(client == null){
           return;
        }

        // 删除在线用户
        redisClient.del(RedisEnum.ONLINE_USER.getKey() + onlineUser);
        redisClient.srem(RedisEnum.ONLINE_NODE_USER.getKey() + clientId, onlineUser);

        // todo 通知在线好友 用户下线
    }
}
