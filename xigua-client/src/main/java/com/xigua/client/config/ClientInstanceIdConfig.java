package com.xigua.client.config;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.xigua.common.core.util.NetUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName ClientInstanceIdConfig
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/1/5 22:57
 */
@Configuration
public class ClientInstanceIdConfig {
    private static final String INSTANCE_NAME = "xigua-client";
    private static final String INSTANCE_NUMBER_KEY = "instance.number";
    private static final String DEFAULT_INSTANCE_NUMBER = "001";

    @Bean
    public NacosDiscoveryProperties nacosDiscoveryProperties() {
        NacosDiscoveryProperties properties = new NacosDiscoveryProperties();

        String number = System.getProperty(
                INSTANCE_NUMBER_KEY,
                DEFAULT_INSTANCE_NUMBER
        );

        // 自定义实例ID：xigua-client#001
        String instanceId = INSTANCE_NAME + "#" + String.format("%03d",
                Integer.parseInt(number.trim()));

        // 更新nacos元数据
        Map<String, String> metadata = properties.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("clientId", instanceId);
        metadata.put("clientIp", NetUtil.getLocalAddress());
        properties.setMetadata(metadata);
        return properties;
    }
}
