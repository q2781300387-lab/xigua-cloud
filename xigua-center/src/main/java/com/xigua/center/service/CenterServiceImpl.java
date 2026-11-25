package com.xigua.center.service;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.center.wsMessage.MessageServiceFactory;
import com.xigua.center.wsMessage.AbstractMessageService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.connect.Client;
import com.xigua.domain.ws.MessageRequest;
import com.xigua.domain.enums.MessageSubType;
import com.xigua.domain.enums.MessageType;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.api.service.CenterService;
import com.xigua.domain.ws.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * @ClassName CenterServiceImpl
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/4/20 10:12
 */
@Slf4j
@Service
@DubboService
public class CenterServiceImpl implements CenterService {
    private static final String ONLINE_USER_CLIENTS = "ONLINE_USER:CLIENTS:";
    @Autowired
    private RedisClient redisClient;

    @Autowired
    private ClientPushService clientPushService;

    /**
     * 客户端注册上线
     * @author wangjinfei
     * @date 2025/4/20 10:23
     * @param client
     */
    @Override
    public void clientRegister(Client client, String userId) {
        // 客户端ws节点信息存储到redis
        String host = client.getHost();
        Integer port = client.getPort();


        /**
         * todo可能服务器直接挂了，但redis还存储着在线用户
         * 需要维护一个过期时间,或者心跳检测
        */
        // 存储在线用户：旧 key 保留一个节点做兼容，set 记录该用户所有节点，支持多实例/多端
        String clientJson = JSONObject.toJSONString(client);
        redisClient.set(RedisEnum.ONLINE_USER.getKey() + userId, clientJson);
        redisClient.sadd(ONLINE_USER_CLIENTS + userId, clientJson);
        // 立即写入心跳时间。否则新连接在首个 ping 到达前会被巡检任务判定为“无心跳”并强制下线，
        // 表现为手机能发消息却收不到任何推送（视频通话 accept / offer 全部丢失）。
        redisClient.set(RedisEnum.LAST_PING_TIME.getKey() + userId, String.valueOf(System.currentTimeMillis()));
        redisClient.sadd(RedisEnum.ONLINE_NODE_USER.getKey() + host, userId);

        // 通知好友用户上线
        MessageRequest messageRequest = new MessageRequest();
        messageRequest.setSenderId(userId);

        AbstractMessageService messageService = MessageServiceFactory.getMessageService(MessageType.NOTIFY.getType(), MessageSubType.FRIEND_ONLINE.getType());
        if(messageService == null){
            log.error("未找到消息服务：消息主类型：{}，消息子类型：{}", MessageType.NOTIFY.getType(), MessageSubType.FRIEND_ONLINE.getType());
            throw new BusinessException("未找到消息服务");
        }
        messageService.handleMessage(messageRequest);
        log.info("------->>>>>>> 用户：{}，所在服务器：{}，注册上线", userId, host);
    }

    /**
     * 客户端注销下线
     * @author wangjinfei
     * @date 2025/4/24 15:41
     * @param client
     * @param userId
     */
    @Override
    public void clientDeregister(Client client, String userId) {
        String host = client.getHost();
        Integer port = client.getPort();

        // redis移除当前节点；若还有其他节点，旧 key 指向剩余节点
        String clientJson = JSONObject.toJSONString(client);
        redisClient.srem(ONLINE_USER_CLIENTS + userId, clientJson);
        Set<Object> remaining = redisClient.members(ONLINE_USER_CLIENTS + userId);
        if (remaining == null || remaining.isEmpty()) {
            redisClient.del(RedisEnum.ONLINE_USER.getKey() + userId);
            redisClient.del(RedisEnum.LAST_PING_TIME.getKey() + userId);
        } else {
            redisClient.set(RedisEnum.ONLINE_USER.getKey() + userId, String.valueOf(remaining.iterator().next()));
        }
        redisClient.srem(RedisEnum.ONLINE_NODE_USER.getKey() + host, userId);

        // redis删除当前用户的活跃好友
        String currentActiveFriendKey = RedisEnum.CURRENT_CHAT_WINDOW.getKey() + userId;
        redisClient.del(currentActiveFriendKey);

        // 通知好友用户下线
        MessageRequest messageRequest = new MessageRequest();
        messageRequest.setSenderId(userId);

        AbstractMessageService messageService = MessageServiceFactory.getMessageService(MessageType.NOTIFY.getType(), MessageSubType.FRIEND_OFFLINE.getType());
        if(messageService == null){
            log.error("未找到消息服务：消息主类型：{}，消息子类型：{}", MessageType.NOTIFY.getType(), MessageSubType.FRIEND_OFFLINE.getType());
            throw new BusinessException("未找到消息服务");
        }
        messageService.handleMessage(messageRequest);

        log.info("------->>>>>>> 用户：{}，所在服务器：{}，注销下线", userId, host);
    }

    /**
     * 接收来自客户端的消息
     * @author wangjinfei
     * @date 2025/4/20 15:06
     * @param messageRequest
     */
    @Override
    public void receiveMessage4Client(MessageRequest messageRequest) {
        String messageType = messageRequest.getMessageType();
        String subType = messageRequest.getSubType();
        if(messageRequest.getServerReceiveTime() == null){
            messageRequest.setServerReceiveTime(System.currentTimeMillis());
        }

        log.info("------->>>>>>> 接收到客户端消息：{}", messageRequest);

        // 根据主类和子类型获取消息service
        AbstractMessageService messageService = MessageServiceFactory.getMessageService(messageType, subType);
        if(messageService == null){
            log.error("未找到消息服务：消息主类型：{}，消息子类型：{}", messageType, subType);
            throw new BusinessException("未找到消息服务");
        }

        // 根据不同的消息service处理消息
        messageService.handleMessage(messageRequest);
    }

    /**
     * 用户是否在线
     * @author wangjinfei
     * @date 2025/4/20 11:42
     * @param userId
     * @return Boolean
     */
    public Client onlineUser(String userId) {
        // 获取在线用户所在的节点信息
        Client client = redisClient.get(RedisEnum.ONLINE_USER.getKey() + userId, Client.class);
        return client;
    }

    /**
     * 发送消息到指定节点
     * @author wangjinfei
     * @date 2025/4/20 11:11
     * @param messageResponse
     */
    @Override
    public void sendMessage2Client(MessageResponse messageResponse, Client client) {
        clientPushService.push(client, messageResponse);
    }

    @Override
    public void sendMessage2User(MessageResponse messageResponse, String userId) {
        Set<Object> members = redisClient.members(ONLINE_USER_CLIENTS + userId);
        if (members == null || members.isEmpty()) {
            Client client = onlineUser(userId);
            if (client != null) {
                clientPushService.push(client, messageResponse);
            }
            return;
        }
        for (Object member : members) {
            try {
                Client client = JSONObject.parseObject(String.valueOf(member), Client.class);
                if (client != null) {
                    clientPushService.push(client, messageResponse);
                }
            } catch (Exception e) {
                log.warn("推送用户 {} 的节点失败: {}", userId, e.getMessage());
            }
        }
    }

    /**
     * 根据用户id判断是否在线
     * @author wangjinfei
     * @date 2025/5/18 14:12
     * @param userId
     * @return Boolean
     */
    @Override
    public Boolean isOnline(String userId) {
        Client client = onlineUser(userId);
        if(client == null){
            return false;
        }
        return true;
    }
}
