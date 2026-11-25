package com.xigua.center.helper;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @ClassName ClientNodeHelper
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/1/05 22:09
 */
public class ClientNodeHelper {
    private static Map<String, ClientNode> clientNodeMap = new ConcurrentHashMap<>();

    public static void put(String clientId, ClientNode client) {
        clientNodeMap.put(clientId, client);
    }

    public static ClientNode get(String clientId) {
        return clientNodeMap.get(clientId);
    }

    public static void remove(String clientId) {
        clientNodeMap.remove(clientId);
    }

    public static void removeAll() {
        clientNodeMap.clear();
    }

    public static Set<String> getAllClientIds(){
        return clientNodeMap.keySet();
    }

    public static Set<String> getAllClientIps() {
        return clientNodeMap.values().stream()
                .map(ClientNode::getIp)
                .collect(Collectors.toSet());
    }
}
