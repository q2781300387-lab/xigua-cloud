package com.xigua.ai.llm.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName DeepseekLLMProperties
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/9/20 14:33
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm.deepseek")
public class DeepseekLLMProperties {
    /** API 基础地址（OpenAI 兼容），默认 DeepSeek 官方 */
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey;
    private String model;
}
