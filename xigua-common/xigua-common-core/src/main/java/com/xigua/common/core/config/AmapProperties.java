package com.xigua.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 高德地图配置属性
 *
 * @author Claude
 * @date 2026/03/20
 */
@Data
@ConfigurationProperties(prefix = "amap")
public class AmapProperties {

    /**
     * 高德地图 API Key
     */
    private String apiKey;

    /**
     * 是否启用高德地图服务
     */
    private boolean enabled = true;
}
