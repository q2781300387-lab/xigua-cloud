package com.xigua.ai.llm;

import com.xigua.ai.client.DeepSeekClient;
import com.xigua.ai.llm.model.ChatContext;
import com.xigua.ai.llm.properties.DeepseekLLMProperties;
import com.xigua.ai.llm.properties.LLMProperties;
import com.xigua.ai.openai.ChatCompletionRequest;
import com.xigua.ai.openai.ChatCompletionResponse;
import com.xigua.ai.sse.StreamCallback;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName DeepSeekLLMService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/9/20 13:27
 */
@Slf4j
@Service
public class DeepSeekLLMService extends AbstractLLMService {
    private LLMProperties llmProperties;
    private DeepseekLLMProperties deepseekLLMProperties;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("deepSeekClient")
    private DeepSeekClient deepSeekClient;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("deepSeekOfficialClient")
    private DeepSeekClient deepSeekOfficialClient;

    public DeepSeekLLMService(LLMProperties llmProperties, DeepseekLLMProperties properties) {
        super(llmProperties);
        this.llmProperties = llmProperties;
        this.deepseekLLMProperties = properties;
    }


    @Override
    public Flux<String> chat(ChatContext chatContext) {
        Flux<String> output = Flux.empty();
        Boolean stream = chatContext.getStream();

        // 设置响应格式
        ChatCompletionRequest.ResponseFormat responseFormat =
                new ChatCompletionRequest.ResponseFormat();
        responseFormat.setType("text");

        List<ChatCompletionRequest.Message> messages = new ArrayList<>();
        if(CollectionUtils.isEmpty(chatContext.getMemory())){
            // 设置系统消息
            ChatCompletionRequest.Message systemMessage = new ChatCompletionRequest.Message();
            systemMessage.setRole("system");
            systemMessage.setContent(chatContext.getPrompt());
            messages.add(systemMessage);

            // 用户input
            ChatCompletionRequest.Message userMessage = new ChatCompletionRequest.Message();
            userMessage.setRole("user");
            userMessage.setContent(chatContext.getInput());
            messages.add(userMessage);
        }else{
            messages = chatContext.getMemory().stream()
                    .map(memory -> {
                        ChatCompletionRequest.Message message = new ChatCompletionRequest.Message();
                        message.setRole(memory.getRole());
                        message.setContent(memory.getContent());
                        return message;
                    })
                    .collect(Collectors.toList());
        }

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(resolveModel(chatContext))
                .maxTokens(llmProperties.getMaxTokens())
                .temperature(llmProperties.getTemperature())
                .frequencyPenalty(0.0)
                .presencePenalty(0.0)
                .responseFormat(responseFormat)
                .messages(messages)
                .build();

        if(stream){
           output = adaptStream(request, resolveModel(chatContext));
        }else{
            ChatCompletionResponse response = resolveClient(resolveModel(chatContext)).chatCompletions(request);
            if(response == null){
                return output;
            }
            if(CollectionUtils.isEmpty(response.getChoices())){
                return output;
            }

            output = Flux.just(response.getChoices().get(0).getMessage().getContent());
        }
        return output;
    }

    private Flux<String> adaptStream(ChatCompletionRequest request, String model) {
        return Flux.create(sink -> {
            StreamCallback streamCallback = new StreamCallback() {
                @Override
                public void onMessage(String content) {
                    sink.next(content);
                }

                @Override
                public void onComplete() {
                    sink.complete();
                }

                @Override
                public void onError(Throwable t) {
                    sink.error(t);
                }
            };

            resolveClient(model).chatCompletionsStream(request, streamCallback);
        });
    }

    /**
     * 解析本次调用使用的模型：优先取会话选择的模型，为空时回退全局默认模型
     */
    private String resolveModel(ChatContext chatContext) {
        if (chatContext != null && StringUtils.isNotBlank(chatContext.getModel())) {
            return chatContext.getModel();
        }
        return deepseekLLMProperties.getModel();
    }

    /**
     * 按模型名路由供应商：deepseek 前缀 → DeepSeek 官方；其余（glm 等）→ 主配置客户端
     */
    private DeepSeekClient resolveClient(String model) {
        if (StringUtils.isNotBlank(model) && model.toLowerCase().startsWith("deepseek")) {
            return deepSeekOfficialClient;
        }
        return deepSeekClient;
    }
}
