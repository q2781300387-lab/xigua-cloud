package com.xigua.ai.service;

import com.xigua.ai.domain.dto.ChatRequest;
import com.xigua.ai.llm.model.ChatContext;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * @ClassName AIService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/9/20 13:07
 */
public interface AIService {
    /**
     * ai对话
     */
    Flux<String> chat(ChatContext chatContext);
    Flux<ServerSentEvent<String>> chatSession(ChatRequest request);
}
