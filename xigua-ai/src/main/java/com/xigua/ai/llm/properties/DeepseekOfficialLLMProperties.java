package com.xigua.ai.llm.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 官方供应商配置（llm.deepseek-official.*）
 * 与主配置 llm.deepseek（当前指向智谱 GLM）分开，用于按会话切换供应商
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm.deepseek-official")
public class DeepseekOfficialLLMProperties {
    /** API 基础地址（OpenAI 兼容） */
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey;
    private String model;
}
