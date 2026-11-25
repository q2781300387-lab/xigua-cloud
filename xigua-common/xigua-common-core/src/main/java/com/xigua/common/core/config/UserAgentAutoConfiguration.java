package com.xigua.common.core.config;

import com.xigua.common.core.util.UserAgentUtil;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * UserAgent 解析器自动配置
 *
 * @author wangjinfei
 * @date 2026/03/22
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "useragent", name = "enabled", havingValue = "true", matchIfMissing = false)
public class UserAgentAutoConfiguration {

    @Bean
    public UserAgentAnalyzer userAgentAnalyzer() {
        log.info("开始初始化 YAUAA UserAgentAnalyzer...");

        long startTime = System.currentTimeMillis();

        UserAgentAnalyzer analyzer = UserAgentAnalyzer
                .newBuilder()
                .hideMatcherLoadStats()
                .withCache(10000)
                .build();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("YAUAA UserAgentAnalyzer 初始化完成，耗时: {}ms", elapsed);

        // 设置到工具类
        UserAgentUtil.setAnalyzer(analyzer);

        return analyzer;
    }
}
