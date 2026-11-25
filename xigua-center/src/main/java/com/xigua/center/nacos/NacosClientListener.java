package com.xigua.center.nacos;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.xigua.api.service.admin.NodeLogService;
import com.xigua.api.service.admin.NodeService;
import com.xigua.center.helper.ClientNode;
import com.xigua.center.helper.ClientNodeHelper;
import com.xigua.common.core.util.NetUtil;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.connect.Client;
import com.xigua.domain.entity.Node;
import com.xigua.domain.enums.RedisEnum;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @ClassName NacosClientListener
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/1/5 22:30
 */
@Slf4j
@Component
public class NacosClientListener {
    private static final String SERVER_CLIENT = "xigua-client";
    private static String CENTER_IP;
    private static Integer CLIENT_DUBBO_PORT;

    @Autowired
    private NamingService namingService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private Environment env;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeLogService nodeLogService;

    @PostConstruct
    public void init() throws NacosException {
        CENTER_IP = getCenterIp();;
        CLIENT_DUBBO_PORT = getClientDubboPort();

        namingService.subscribe(SERVER_CLIENT, event -> {
            if (event instanceof NamingEvent namingEvent) {
                List<Instance> instances = namingEvent.getInstances();

                // 这里就是最新的实例列表 （全量的）
                log.info(SERVER_CLIENT + "实例节点：{}", instances);
                handleInstanceChange(instances);
            }
        });
    }

    private void handleInstanceChange(List<Instance> instances) {
        if(CollectionUtils.isEmpty(instances)){
            log.info(SERVER_CLIENT + "实例为空，清除所有客户端节点");

            // 记录所有节点下线
            Set<String> allClientIds = ClientNodeHelper.getAllClientIds();
            for (String clientId : allClientIds) {
                ClientNode clientNode = ClientNodeHelper.get(clientId);
                if (clientNode != null) {
                    nodeOffline(clientNode.getIp());
                }
            }

            ClientNodeHelper.removeAll();
            return;
        }

        // 先获取当前的客户端ID集合
        Set<String> currentClientIds = ClientNodeHelper.getAllClientIds();

        // 用于记录新的客户端ID
        Set<String> newClientIds = new HashSet<>();

        // 更新或添加新节点
        for (Instance instance : instances) {
            Map<String, String> metadata = instance.getMetadata();
            String clientId = metadata.get("clientId");
            // 不用instance.getIp()，如果nacos用docker部署instance.getIp()获取到的是容器ip
            // 获取ip的方法得全部统一
            String clientIp = metadata.get("clientIp");
            int clientPort = instance.getPort();

            if (clientId != null && !clientId.trim().isEmpty()) {
                ClientNode clientNode = ClientNode.builder()
                        .clientId(clientId)
                        .ip(clientIp)
                        .port(clientPort)
                        .centerIp(CENTER_IP)
                        .build();

                log.info(SERVER_CLIENT + "更新节点：{}", clientNode);

                // 更新内存
                ClientNodeHelper.put(clientId, clientNode);

                // 记录节点上线
                nodeOnline(clientIp);

                newClientIds.add(clientId);
            }
        }

        // 移除已下线的节点
        Set<String> removeClientIds = currentClientIds.stream()
                .filter(clientId -> !newClientIds.contains(clientId))
                .collect(Collectors.toSet());
        for (String removeClientId : removeClientIds) {
            log.info(SERVER_CLIENT + "移除节点：{}", removeClientId);

            ClientNode clientNode = ClientNodeHelper.get(removeClientId);
            ClientNodeHelper.remove(removeClientId);

            // 记录节点下线
            nodeOffline(clientNode.getIp());
        }

        log.info(SERVER_CLIENT + "实例更新完成，当前客户端节点数: {}", newClientIds.size());
    }

    /**
     * 节点上线
     * @param innerIp 内网IP
     */
    private void nodeOnline(String innerIp) {
        Boolean online = nodeService.online(innerIp);
        if (!online) {
            log.info("节点上线失败，节点IP: {}", innerIp);
            return;
        }
        log.info("节点上线成功，节点IP: {}", innerIp);
    }

    /**
     * 节点下线
     * @param innerIp 内网IP
     */
    private void nodeOffline(String innerIp) {
        Boolean online = nodeService.offline(innerIp);
        if (!online) {
            log.info("节点下线失败，节点IP: {}", innerIp);
            return;
        }
        log.info("节点下线成功，节点IP: {}", innerIp);
    }

    private String getCenterIp() throws NacosException {
        // 获取自己的内网ip
        // center 暂不考虑多节点（分布式），太复杂
       return NetUtil.getLocalAddress();
    }

    private Integer getClientDubboPort() throws NacosException {
        String active = env.getProperty("spring.profiles.active", "");
        // 尝试获取不同格式的配置
        String[] possibleDataIds = {
                SERVER_CLIENT + "-" + active + ".yaml",
                SERVER_CLIENT + "-" + active + ".yml",
                SERVER_CLIENT + "-" + active + ".properties",
        };
        try {
            for (String dataId : possibleDataIds) {
                String configContent = configService.getConfig(dataId, "DEFAULT_GROUP", 3000);
                if (configContent != null && !configContent.isEmpty()) {
                    Integer port = extractDubboPortFromConfig(configContent, dataId);
                    if (port != null && port > 0) {
                        return port;
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取Dubbo配置失败，serviceName: {}", SERVER_CLIENT, e);
        }

        return -1; // 默认值
    }

    private Integer extractDubboPortFromConfig(String configContent, String dataId) {
        try {
            // 只处理 yaml/yml 格式，properties暂不扩展
            if (dataId.toLowerCase().endsWith(".yaml") ||
                    dataId.toLowerCase().endsWith(".yml")) {

                Yaml yaml = new Yaml();
                Map<String, Object> config = yaml.load(configContent);

                // 直接按照配置结构获取
                // 配置结构: dubbo -> protocol -> port
                Map<String, Object> dubboConfig = (Map<String, Object>) config.get("dubbo");
                if (dubboConfig != null) {
                    Map<String, Object> protocolConfig = (Map<String, Object>) dubboConfig.get("protocol");
                    if (protocolConfig != null) {
                        Object portObj = protocolConfig.get("port");
                        if (portObj instanceof Integer) {
                            return (Integer) portObj;
                        } else if (portObj instanceof String) {
                            return Integer.parseInt((String) portObj);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析dubbo.protocol.port失败", e);
        }

        return -1;
    }
}
