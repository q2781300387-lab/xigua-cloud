package com.xigua.common.es.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName EsAutoConfiguration
 * @Description Elasticsearch 自动配置类
 * @Author wangjinfei
 * @Date 2026/9/29
 */
@Configuration
@EnableConfigurationProperties(EsProperties.class)
@ConditionalOnClass(ElasticsearchClient.class)
@ConditionalOnProperty(prefix = "xigua.es", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestClient esRestClient(EsProperties properties) {
        HttpHost[] hosts = properties.getUris().stream()
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);

        RestClientBuilder builder = RestClient.builder(hosts)
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(properties.getConnectTimeout())
                        .setSocketTimeout(properties.getSocketTimeout()));

        boolean authEnabled = StringUtils.isNotBlank(properties.getUsername());
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setMaxConnTotal(properties.getMaxConnTotal())
                    .setMaxConnPerRoute(properties.getMaxConnPerRoute());

            if (authEnabled) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }

            return httpClientBuilder;
        });

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchTransport esTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
