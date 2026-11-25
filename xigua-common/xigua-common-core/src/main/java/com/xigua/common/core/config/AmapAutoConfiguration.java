package com.xigua.common.core.config;

import com.xigua.common.core.util.AmapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 高德地图自动配置
 *
 * @author Claude
 * @date 2026/03/20
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AmapProperties.class)
@ConditionalOnProperty(prefix = "amap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AmapAutoConfiguration implements InitializingBean {

    private final AmapProperties amapProperties;

    public AmapAutoConfiguration(AmapProperties amapProperties) {
        this.amapProperties = amapProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String apiKey = amapProperties.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("高德地图 API Key 未配置，IP 定位功能将不可用");
            return;
        }

        // 设置高德地图 API Key
        AmapUtil.setApiKey(apiKey);
        log.info("高德地图 API Key 配置成功");
    }
}
