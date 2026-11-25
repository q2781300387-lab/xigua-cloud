//package com.xigua.client.ws;
//
//import com.alibaba.fastjson2.JSONObject;
//import com.xigua.client.config.CustomWebSocketConfigurator;
//import com.xigua.client.helper.WsSessionHelper;
//import com.xigua.common.core.exception.BusinessException;
//import com.xigua.common.redis.client.RedisClient;
//import com.xigua.domain.entity.User;
//import com.xigua.domain.enums.RedisEnum;
//import com.xigua.domain.ws.MessageRequest;
//import com.xigua.api.service.ClientService;
//import jakarta.websocket.*;
//import jakarta.websocket.server.PathParam;
//import jakarta.websocket.server.ServerEndpoint;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.dubbo.config.annotation.DubboReference;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
///**
// * @ClassName ConnectController
// * @Description TODO
// * @Author wangjinfei
// * @Date 2025/4/19 9:54
// */
//@Data
//@Slf4j
//@Component
//@ServerEndpoint(value = "/connect/{userId}", configurator = CustomWebSocketConfigurator.class)
//public class WsServer {
//    private static ClientService clientService;
//    private static RedisClient redisClient;
//    @DubboReference
//    public void setClientService(ClientService clientService) {
//        WsServer.clientService = clientService;
//    }
//    @Autowired
//    public void setRedisClient(RedisClient redisClient) {
//        WsServer.redisClient = redisClient;
//    }
//
//    // 连接打开
//    @OnOpen
//    public void onOpen(@PathParam("userId") String userId, Session session, EndpointConfig config){
//        String ip = (String) session.getUserProperties().get("clientIp");
//        // todo 可以根据userId判断是否是合法用户，达到token鉴权的目的
//        String userCache = redisClient.get(RedisEnum.USER.getKey() + userId);
//        if(StringUtils.isEmpty(userCache)){
//            log.info("---->>>>>[websocket] 非法连接：ip={}，userId={}，不是有效用户无法连接", ip, userId);
//            try {
//                CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,"不是有效用户，无法连接");
//                session.close(closeReason);
//            } catch (Exception e) {
//                log.error("关闭非法连接失败", e);
//            }
//            return;
//        }
//
//        // 根据用户保存session
//        WsSessionHelper.put(userId,session);
//        log.info("---->>>>>[websocket] 新的连接：ip={}，userId={}", ip, userId);
//        // todo 绑定token
//        WsSessionHelper.buildToken(JSONObject.parseObject(userCache, User.class));
//
//        // 当前用户所连接的ws节点信息注册到长连接服务器
//        try {
//            clientService.clientRegister2Center(userId);
//        } catch (Exception e) {
//            log.error("---->>>>>[websocket] 注册用户到长连接服务器系统异常：", e);
//        } finally {
//            WsSessionHelper.unBuildToken();
//        }
//    }
//
//    // 连接关闭
//    @OnClose
//    public void onClose(@PathParam("userId") String userId, CloseReason closeReason){
//        WsSessionHelper.remove(userId);
//        log.info("---->>>>>[websocket] 连接断开：userId={}，reason={}",userId,closeReason);
//
//        String userCache = redisClient.get(RedisEnum.USER.getKey() + userId);
//        if(StringUtils.isEmpty(userCache)){
//            return;
//        }
//
//        // 当前用户所连接的ws节点信息注销到长连接服务器
//        clientService.clientDeregister2Center(userId);
//    }
//
//    // 收到消息
//    @OnMessage
//    public void onMessage(String message, Session session) throws IOException {
//        String ip = (String) session.getUserProperties().get("clientIp");
//        log.info("---->>>>>[websocket] 收到消息：sessionId={}，ip={}，message={}", session.getId(), ip, message);
//        MessageRequest messageRequest = JSONObject.parseObject(message, MessageRequest.class);
//
//        // 服务端收到消息的时间
//        messageRequest.setServerReceiveTime(System.currentTimeMillis());
//        messageRequest.setIp(ip);
//
//        // todo 绑定token
//
//        // 发消息到长连接服务器
//        try{
//            clientService.sendMessage2Center(messageRequest);
//        }catch (BusinessException e){
//            log.error("---->>>>>[websocket] 发送消息到长连接服务器业务异常：", e);
//        }catch (Exception e){
//            log.error("---->>>>>[websocket] 发送消息到长连接服务器系统异常：", e);
//        }
//    }
//
//    // 连接异常
//    @OnError
//    public void onError(Session session, Throwable throwable) throws IOException {
//        log.info("---->>>>>[websocket] 连接异常：", throwable);
//    }
//}
