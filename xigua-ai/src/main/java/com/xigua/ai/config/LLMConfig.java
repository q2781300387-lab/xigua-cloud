package com.xigua.ai.config;

import com.xigua.ai.client.CogViewClient;
import com.xigua.ai.client.DeepSeekClient;
import com.xigua.ai.llm.properties.DeepseekLLMProperties;
import com.xigua.ai.llm.properties.DeepseekOfficialLLMProperties;
import com.xigua.common.sequence.properties.SequenceSnowflakeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName LLMConfig
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/9/20 21:37
 */
@Slf4j
@Configuration
public class LLMConfig {
    @Bean
    @ConditionalOnBean(DeepseekLLMProperties.class)
    @ConditionalOnProperty(prefix = "llm", name = "provider", havingValue = "deepseek")
    public DeepSeekClient deepSeekClient(DeepseekLLMProperties properties){
        log.info("DeepSeekClient init, model: {}", properties.getModel());
        DeepSeekClient client = DeepSeekClient.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .model(properties.getModel())
                .build();
        log.info("DeepSeekClient success");
        return client;
    }

    /**
     * 智谱 CogView 文生图客户端（复用主配置 llm.deepseek 的智谱 api-key，model 固定 cogview-3-flash）
     */
    @Bean
    @ConditionalOnBean(DeepseekLLMProperties.class)
    public CogViewClient cogViewClient(DeepseekLLMProperties properties){
        log.info("CogViewClient init, model: cogview-3-flash");
        CogViewClient client = CogViewClient.builder()
                .apiKey(properties.getApiKey())
                .model("cogview-3-flash")
                .build();
        log.info("CogViewClient success");
        return client;
    }

    /**
     * DeepSeek 官方客户端（llm.deepseek-official.*），用于按会话切换供应商
     */
    @Bean
    @ConditionalOnBean(DeepseekOfficialLLMProperties.class)
    public DeepSeekClient deepSeekOfficialClient(DeepseekOfficialLLMProperties properties){
        log.info("DeepSeekOfficialClient init, model: {}", properties.getModel());
        DeepSeekClient client = DeepSeekClient.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .model(properties.getModel())
                .build();
        log.info("DeepSeekOfficialClient success");
        return client;
    }
}
