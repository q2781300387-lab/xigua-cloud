package com.xigua.common.es.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @ClassName EsProperties
 * @Description Elasticsearch 配置属性
 * @Author wangjinfei
 * @Date 2026/9/29
 */
@Data
@ConfigurationProperties(prefix = "xigua.es")
public class EsProperties {

    /**
     * 是否启用 Elasticsearch
     */
    private boolean enabled = true;

    /**
     * 节点地址列表，如 http://127.0.0.1:9200
     */
    private List<String> uris = List.of("http://127.0.0.1:9200");

    /**
     * 用户名（未开启安全认证可不填）
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * socket 超时时间（毫秒）
     */
    private int socketTimeout = 60000;

    /**
     * 最大连接数
     */
    private int maxConnTotal = 100;

    /**
     * 单路由最大连接数
     */
    private int maxConnPerRoute = 100;
}
