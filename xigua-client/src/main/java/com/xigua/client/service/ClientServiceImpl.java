package com.xigua.client.service;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.client.helper.WsSessionHelper;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.NetUtil;
import com.xigua.domain.connect.Client;
import com.xigua.domain.ws.MessageRequest;
import com.xigua.api.service.ClientService;
import com.xigua.api.service.CenterService;
import com.xigua.domain.ws.MessageResponse;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;

/**
 * @ClassName ClientServiceImpl
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/4/20 15:33
 */
@Slf4j
@Service
@DubboService
public class ClientServiceImpl implements ClientService {
    @Autowired
    private ServletWebServerApplicationContext webServerAppContext;
    @DubboReference
    private CenterService centerService;

    @Value("${dubbo.protocol.port}")
    private Integer dubboPort;

    /**
     * 当前用户所连接的ws节点信息注册到长连接服务器
     * @author wangjinfei
     * @date 2025/4/20 15:43
     * @param userId
     * @return Boolean
     */
    @Override
    public Boolean clientRegister2Center(String userId) {
        // 获取当前服务ip和端口
        int port = webServerAppContext.getWebServer().getPort();
        String host = null;
        try {
            // 获取本机的 IP 地址
            host = NetUtil.getLocalAddress();
            // 多网卡 / 虚拟网卡环境下可能取到 169.254.x.x 链路本地地址，该地址不可路由，
            // center 用它对节点做点对点 RPC 会直接失败，消息推送静默丢失
            if (host != null && host.startsWith("169.254.")) {
                log.warn("----->>>>> 取到链路本地地址 {}，回退到 127.0.0.1 保证节点可达", host);
                host = "127.0.0.1";
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("----->>>>> 获取ip失败", e);
        }
        if(StringUtils.isEmpty(host)){
            log.error("----->>>>> 获取ip失败");
            throw new BusinessException("获取ip失败");
        }

        log.info("----->>>>> host:{},port:{}", host, port);

        WebSocketSession session = WsSessionHelper.get(userId);
        Client client = new Client();
        client.setHost(host);
        client.setPort(port);
        client.setDubboPort(dubboPort);
        client.setLoginTime(System.currentTimeMillis());
        client.setLoginIp((String) session.getAttributes().get("clientIp"));
        client.setUa((String) session.getAttributes().get("ua"));
        // ws节点信息注册到服务端
        centerService.clientRegister(client, userId);
        return true;
    }

    /**
     * 当前用户所连接的ws节点信息注销到长连接服务器
     * @author wangjinfei
     * @date 2025/4/24 15:36
     * @param userId
     * @return Boolean
     */
    @Override
    public Boolean clientDeregister2Center(String userId) {
        // 获取当前服务ip和端口
        int port = webServerAppContext.getWebServer().getPort();
        String host = null;
        try {
            // 获取本机的 IP 地址
            host = NetUtil.getLocalAddress();
            // 多网卡 / 虚拟网卡环境下可能取到 169.254.x.x 链路本地地址，该地址不可路由，
            // center 用它对节点做点对点 RPC 会直接失败，消息推送静默丢失
            if (host != null && host.startsWith("169.254.")) {
                log.warn("----->>>>> 取到链路本地地址 {}，回退到 127.0.0.1 保证节点可达", host);
                host = "127.0.0.1";
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("----->>>>> 获取ip失败", e);
        }
        if(StringUtils.isEmpty(host)){
            log.error("----->>>>> 获取ip失败");
            throw new BusinessException("获取ip失败");
        }

        log.info("----->>>>> host:{},port:{}", host, port);

        Client client = new Client();
        client.setHost(host);
        client.setPort(port);
        client.setDubboPort(dubboPort);

        // ws节点信息从服务端注销
        centerService.clientDeregister(client, userId);
        return true;
    }

    /**
     * 发消息到长连接服务器
     * @author wangjinfei
     * @date 2025/4/20 15:58
     * @param messageRequest
     */
    @Override
    public void sendMessage2Center(MessageRequest messageRequest) {
        centerService.receiveMessage4Client(messageRequest);
    }

    /**
     * 接收来自长连接服务器的消息
     * @author wangjinfei
     * @date 2025/4/20 19:27
     * @param messageResponse
     */
    @Override
    public void receiveMessage4Center(MessageResponse messageResponse) {
        String receiverId = messageResponse.getReceiverId();

        // 多端同步：向接收人的所有在线连接推送（电脑/手机/多标签页同时收到）
        Collection<WebSocketSession> sessions = WsSessionHelper.getSessions(receiverId);
        log.info("----->>>>>[push] 收到中心节点推送：receiverId={}，在线连接数={}，subType={}",
                receiverId, sessions.size(), messageResponse.getSubType());
        if (sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions) {
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(JSONObject.toJSONString(messageResponse)));
                    log.info("----->>>>>[push] 已下发：sessionId={}，receiverId={}", session.getId(), receiverId);
                } catch (Exception e) {
                    log.error("发送消息失败，sessionId={}", session.getId(), e);
                }
            } else if (session != null) {
                log.warn("----->>>>>[push] 连接已关闭，跳过：sessionId={}，receiverId={}", session.getId(), receiverId);
            }
        }
    }
}
