package com.xigua.center.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Properties;

/**
 * @ClassName NacosConfig
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/1/5 22:36
 */
@Configuration
public class NacosConfig {
    @Autowired
    private Environment env;

    @Bean
    public NamingService namingService() throws Exception {
        Properties properties = new Properties();
        // 从bootstrap中读取配置，不从nacos中读取nacos本身的配置
        String serverAddr = env.getProperty("spring.cloud.nacos.server-addr", "127.0.0.1:8848");
        String namespace = env.getProperty("spring.cloud.nacos.discovery.namespace", "public");
        properties.put("serverAddr", serverAddr);
        if (StringUtils.isNotBlank(namespace)) {
            properties.put("namespace", namespace);
        }
        return NacosFactory.createNamingService(properties);
    }

    @Bean
    public ConfigService nacosConfigService() throws NacosException {
        Properties properties = new Properties();
        String serverAddr = env.getProperty("spring.cloud.nacos.server-addr", "127.0.0.1:8848");
        String namespace = env.getProperty("spring.cloud.nacos.discovery.namespace", "public");
        String username = env.getProperty("spring.cloud.nacos.username", "nacos");
        String password = env.getProperty("spring.cloud.nacos.password", "nacos");
        properties.put("serverAddr", serverAddr);

        if (namespace != null && !namespace.trim().isEmpty()) {
            properties.put("namespace", namespace);
        }

        if (username != null && !username.trim().isEmpty()) {
            properties.put("username", username);
            properties.put("password", password != null ? password : "");
        }

        return NacosFactory.createConfigService(properties);
    }
}
