package com.xigua.ai.service;

import com.xigua.ai.agent.Agent;
import com.xigua.ai.agent.model.AgentContext;
import com.xigua.ai.domain.dto.ChatRequest;
import com.xigua.ai.llm.LLMService;
import com.xigua.ai.llm.model.ChatContext;
import com.xigua.ai.llm.model.Memory;
import com.xigua.api.service.AiMemoryService;
import com.xigua.api.service.AiMessageService;
import com.xigua.api.service.AiSessionService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.bo.AiMemoryBO;
import com.xigua.domain.entity.AiSession;
import com.xigua.domain.enums.RedisEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName AIServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/24 19:54
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private LLMService llmService;
    @Autowired
    private Agent agent;
    @Autowired
    private Sequence sequence;
    @Autowired
    private AiSessionService aiSessionService;
    @Autowired
    private AiMessageService aiMessageService;
    @Autowired
    private AiMemoryService aiMemoryService;

    @Override
    public Flux<String> chat(ChatContext chatContext) {
        Flux<String> output = null;
        try {
            output = llmService.chat(chatContext);
        } catch (Exception e) {
            log.error("调用LLM服务失败", e);
            throw new BusinessException("调用LLM服务失败");
        }

        return output;
    }

    @Override
    public Flux<ServerSentEvent<String>> chatSession(ChatRequest request) {
        if(StringUtils.isEmpty(request.getSessionId())){
           throw new BusinessException("sessionId不能为空");
        }else{
            // 保存用户对话
            aiMessageService.saveUserMessage(request.getSessionId(), request.getInput());
        }

        // todo 随session带上下文，传入llm （从redis查）
        List<AiMemoryBO> memory = aiMemoryService.refreshMemory(request.getSessionId(), 30);
        List<Memory> memoryList = memory.stream()
                .map(aiMemoryBO -> {
                    Memory mem = new Memory();
                    mem.setRole(aiMemoryBO.getRole());
                    mem.setContent(aiMemoryBO.getContent());
                    return mem;
                })
                .collect(Collectors.toList());

        Flux<ServerSentEvent<String>> output;
        StringBuilder completeOutputBuilder = new StringBuilder();
        try {
            AgentContext agentContext = AgentContext.builder()
                    .requestId(request.getSessionId())
                    .systemPrompt(getSystemPrompt(request.getSessionId()))
                    .model(getSessionModel(request.getSessionId()))
                    .input(request.getInput())
                    .memory(memoryList)
                    .stream(true)
                    .build();
            output = agent.process(agentContext)
                    .doOnNext(event -> {
                        // 只累积 message 类型的内容，跳过 status
                        if ("message".equals(event.event())) {
                            completeOutputBuilder.append(event.data());
                        }
                    })
                    .doOnComplete(() -> {
                        // 持久化
                        String completeOutput = completeOutputBuilder.toString();
                        aiMessageService.saveAiMessage(request.getSessionId(), completeOutput);
                    })
                    .doOnError(e -> {
                        log.error("流处理错误", e);
                    });
        } catch (Exception e) {
            log.error("调用LLM服务失败", e);
            throw new BusinessException("调用LLM服务失败");
        }

        return output;
    }

    /**
     * 获取会话选择的模型
     */
    private String getSessionModel(String sessionId){
        AiSession aiSession = redisClient.get(RedisEnum.AI_SESSION.getKey() + sessionId, AiSession.class);
        if(aiSession != null && StringUtils.isNotEmpty(aiSession.getModel())){
            return aiSession.getModel();
        }

        return null;
    }

    /**
     * 获取系统提示词
     * @author wangjinfei
     * @date 2026/2/25 13:40
     * @param sessionId
     * @return String
     */
    private String getSystemPrompt(String sessionId){
        AiSession aiSession = redisClient.get(RedisEnum.AI_SESSION.getKey() + sessionId, AiSession.class);
        if(aiSession != null){
            return aiSession.getSystemPrompt();
        }

        return "";
    }
}
