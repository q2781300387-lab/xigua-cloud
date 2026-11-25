package com.xigua.ai.agent;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.ai.agent.model.AgentContext;
import com.xigua.ai.llm.model.ChatContext;
import com.xigua.ai.domain.enums.Prompt;
import com.xigua.ai.helper.IntentRecognizer;
import com.xigua.ai.helper.IntentType;
import com.xigua.ai.llm.LLMService;
import com.xigua.ai.llm.model.Memory;
import com.xigua.ai.tool.ToolDispatcher;
import com.xigua.ai.tool.ToolSelector;
import com.xigua.ai.tool.ToolType;
import com.xigua.ai.tool.model.ToolCall;
import com.xigua.ai.tool.model.ToolRequest;
import com.xigua.ai.tool.model.ToolResult;
import com.xigua.ai.tool.model.ToolSelectionResult;
import com.xigua.ai.utils.PromptUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * @ClassName SimpleAgent
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/10/18 19:04
 */
@Slf4j
@Service
public class SimpleAgent implements Agent {
    @Autowired
    private LLMService llmService;
    @Autowired
    private IntentRecognizer intentRecognizer;
    @Autowired
    private ToolDispatcher toolDispatcher;
    @Autowired
    private ToolSelector toolSelector;

    @Override
    public Flux<ServerSentEvent<String>> process(AgentContext agentContext) {
        String requestId = agentContext.getRequestId();
        Boolean stream = agentContext.getStream();
        String input = agentContext.getInput();
        List<Memory> memory = agentContext.getMemory();

        // 推送状态：正在识别意图
        Flux<ServerSentEvent<String>> result = Flux.just(sseStatus("正在识别意图..."));

        IntentType intentType = intentRecognizer.detect(input);
        log.info("agentProcess requestId: {}，意图: {}", requestId, intentType);

        switch (intentType) {
            case UNKNOWN:
                return Flux.concat(result, Flux.just(sseMessage("未知意图")));
            case TEXT_TO_TEXT:
                String systemPrompt = agentContext.getSystemPrompt();
                if(StringUtils.isEmpty(systemPrompt)) {
                    systemPrompt = PromptUtil.getPrompt(Prompt.DEFAULT);
                }
                ChatContext chatContext = ChatContext.builder()
                        .prompt(systemPrompt)
                        .input(input)
                        .memory(memory)
                        .stream(stream)
                        .model(agentContext.getModel())
                        .build();
                try {
                    log.info("agentProcess ai对话开始 requestId: {}，调用模型入参: {}", requestId, chatContext);
                    result = Flux.concat(result, toMessageFlux(llmService.chat(chatContext)));
                    log.info("agentProcess ai对话结束 requestId: {}，调用模型结束", requestId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            case USE_TOOL:
                // llm推理调用工具名称和参数
                ToolSelectionResult toolSelectionResult = toolSelector.selectTool(input);
                log.info("agentProcess requestId: {}，工具选择结果: {}", requestId, toolSelectionResult);
                if(toolSelectionResult == null){
                    return Flux.concat(result, Flux.just(sseMessage("未选择到合适的工具")));
                }

                // 推送状态：正在调用工具
                result = Flux.concat(result, Flux.just(sseStatus("正在调用工具: " + toolSelectionResult.getToolType().getDescription() + "...")));

                // 调用工具
                ToolCall toolCall = ToolCall.builder()
                        .toolName(toolSelectionResult.getToolType().getName())
                        .request(ToolRequest.builder()
                                .params(toolSelectionResult.getParams())
                                .build())
                        .build();
                log.info("agentProcess requestId: {}，工具调用入参: {}", requestId, toolCall);
                ToolResult toolResult = toolDispatcher.dispatch(toolCall);
                log.info("agentProcess requestId: {}，工具调用结果: {}", requestId, toolResult);

                // tool调用结果 再根据llm推理处理一下
                log.info("agentProcess requestId: {}，大模型推理处理工具调用结果", requestId);
                result = Flux.concat(result, Flux.just(sseStatus("正在生成回答...")));
                Map<String, Object> params = Map.<String, Object>of(
                        "user_input", input,
                        "tool_name", toolResult.getToolName(),
                        "tool_output", JSONObject.toJSONString(toolResult.getData()),
                        "stream", stream
                );
                ToolCall refineCall = ToolCall.builder()
                        .toolName(ToolType.REFINE.getName())
                        .request(ToolRequest.builder()
                                .params(params)
                                .build())
                        .build();
                ToolResult refineResult = toolDispatcher.dispatch(refineCall);
                result = Flux.concat(result, Flux.just(sseMessage(refineResult.getOutput())));
                break;
            default:
                return Flux.concat(result, Flux.just(sseMessage("未知意图")));
        }

        return result;
    }

    /**
     * 构建状态类型的 SSE 事件
     */
    private ServerSentEvent<String> sseStatus(String data) {
        return ServerSentEvent.<String>builder().event("status").data(data).build();
    }

    /**
     * 构建消息类型的 SSE 事件
     */
    private ServerSentEvent<String> sseMessage(String data) {
        return ServerSentEvent.<String>builder().event("message").data(data).build();
    }

    /**
     * 将 Flux<String> 中每个元素包装为 message 类型的 SSE 事件
     */
    private Flux<ServerSentEvent<String>> toMessageFlux(Flux<String> flux) {
        return flux.map(chunk -> ServerSentEvent.<String>builder().event("message").data(chunk).build());
    }
}
