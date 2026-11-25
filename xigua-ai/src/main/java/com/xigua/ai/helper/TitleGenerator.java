package com.xigua.ai.helper;

import com.xigua.ai.domain.enums.Prompt;
import com.xigua.ai.llm.LLMService;
import com.xigua.ai.llm.model.ChatContext;
import com.xigua.ai.utils.PromptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @ClassName TitleGenerator
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/26 10:49
 */
@Slf4j
@Component
public class TitleGenerator {
    @Autowired
    private LLMService llmService;

    public String title(String userMessage){
        String prompt = PromptUtil.getPrompt(Prompt.TITLE);
        prompt = PromptUtil.fill(prompt, Map.of("user_input", userMessage));

        try {
            ChatContext chatContext = ChatContext.builder()
                    .input(userMessage)
                    .stream(false)
                    .prompt(prompt)
                    .build();
            // 调模型
            Flux<String> result = llmService.chat(chatContext);
            String title = result.blockFirst();

            // 清洗结果
            return cleanTitle(title);

        } catch (Exception e) {
            // 降级策略
            return fallbackTitle(userMessage);
        }
    }

    private String cleanTitle(String title) {
        if (title == null) return "新对话";

        title = title.replaceAll("[\\n\\r]", "").trim();

        // 长度限制
        if (title.length() > 12) {
            title = title.substring(0, 12);
        }

        return title.isEmpty() ? "新对话" : title;
    }

    private String fallbackTitle(String msg) {
        if (msg == null || msg.isEmpty()) return "新对话";
        return msg.substring(0, Math.min(12, msg.length()));
    }
}
