package com.xigua.common.docker.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.okhttp.OkHttpDockerCmdExecFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName DockerAutoConfiguration
 * @Description Docker 自动配置类
 * @Author Claude Code
 * @Date 2026/5/8
 */
@Configuration
@EnableConfigurationProperties(DockerProperties.class)
@ConditionalOnProperty(prefix = "docker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DockerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DockerClientConfig dockerClientConfig(DockerProperties properties) {
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(properties.getHost());

        if (StringUtils.isNotBlank(properties.getApiVersion())) {
            builder.withApiVersion(properties.getApiVersion());
        }

        if (properties.isTlsVerify() && StringUtils.isNotBlank(properties.getCertPath())) {
            builder.withDockerTlsVerify(true)
                    .withDockerCertPath(properties.getCertPath());
        }

        if (StringUtils.isNotBlank(properties.getRegistryUrl())) {
            builder.withRegistryUrl(properties.getRegistryUrl());
        }

        if (StringUtils.isNotBlank(properties.getRegistryUsername())) {
            builder.withRegistryUsername(properties.getRegistryUsername());
        }

        if (StringUtils.isNotBlank(properties.getRegistryPassword())) {
            builder.withRegistryPassword(properties.getRegistryPassword());
        }

        if (StringUtils.isNotBlank(properties.getRegistryEmail())) {
            builder.withRegistryEmail(properties.getRegistryEmail());
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public DockerClient dockerClient(DockerClientConfig config) {
        OkHttpDockerCmdExecFactory factory = new OkHttpDockerCmdExecFactory()
                .withReadTimeout(30)
                .withConnectTimeout(30);
        return DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(factory).build();
    }
}
