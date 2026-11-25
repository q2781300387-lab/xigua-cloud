package com.xigua.client.ws;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.domain.enums.ChatType;
import com.xigua.domain.enums.MessageSubType;
import com.xigua.domain.enums.MessageType;
import com.xigua.domain.ws.MessageRequest;
import okhttp3.*;

import java.util.concurrent.TimeUnit;

/**
 * @ClassName WebSocketTest
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/10/27 20:15
 */
public class WebSocketTest {
    private static final String BASE_URL = "ws://localhost:8090/client/connect/";

    public static void connect(Integer count) throws InterruptedException {
        // 创建自定义的 OkHttpClient
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(1024);
        dispatcher.setMaxRequestsPerHost(1024);

        OkHttpClient client = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(200, 5, TimeUnit.MINUTES))
                .build();



        for (int i = 1; i <= count; i++) {
            String url = BASE_URL + i;
            Request request = new Request.Builder().url(url).build();

            int finalI = i;
            client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    System.out.println("✅ 连接成功: " + finalI);
                    MessageRequest request = new MessageRequest();
                    request.setSenderId(String.valueOf(finalI));
                    request.setReceiverId(String.valueOf(finalI + 1));
                    request.setMessageType(MessageType.CHAT.getType());
                    request.setSubType(MessageSubType.MES_SEND.getType());
                    request.setMessage("Hello, World!");
                    request.setChatType(ChatType.PRIVATE.getType());
                    webSocket.send(JSONObject.toJSONString(request));
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    System.err.println("❌ 连接失败[" + finalI + "]: " + t.getMessage());

                }
            });

            Thread.sleep(200); // 减少延迟
        }
    }

    public static void main(String[] args) throws InterruptedException {
        connect(100);


        // 保持程序运行
//        Thread.sleep(KEEP_ALIVE_SECONDS * 1000);
    }
}
