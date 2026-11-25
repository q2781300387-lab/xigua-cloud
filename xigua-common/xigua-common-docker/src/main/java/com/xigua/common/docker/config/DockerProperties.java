package com.xigua.common.docker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @ClassName DockerProperties
 * @Description Docker 配置属性
 * @Author Claude Code
 * @Date 2026/5/8
 */
@Data
@ConfigurationProperties(prefix = "docker")
public class DockerProperties {

    /**
     * Docker 守护进程地址
     * 默认 unix:///var/run/docker.sock (Linux/Mac)
     * Windows 使用 tcp://localhost:2375 或 npipe:////./pipe/docker_engine
     */
    private String host = "unix:///var/run/docker.sock";

    /**
     * 是否启用 TLS
     */
    private boolean tlsVerify = false;

    /**
     * Docker 证书路径（启用 TLS 时使用）
     */
    private String certPath;

    /**
     * Docker API 版本
     */
    private String apiVersion;

    /**
     * Docker Registry 地址
     */
    private String registryUrl;

    /**
     * Docker Registry 用户名
     */
    private String registryUsername;

    /**
     * Docker Registry 密码
     */
    private String registryPassword;

    /**
     * Docker Registry 邮箱
     */
    private String registryEmail;
}
