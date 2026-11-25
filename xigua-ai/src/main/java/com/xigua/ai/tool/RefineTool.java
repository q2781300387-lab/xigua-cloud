package com.xigua.ai.tool;

import com.xigua.ai.domain.enums.Prompt;
import com.xigua.ai.domain.enums.RoleType;
import com.xigua.ai.llm.LLMService;
import com.xigua.ai.llm.model.ChatContext;
import com.xigua.ai.llm.model.Memory;
import com.xigua.ai.tool.model.ToolRequest;
import com.xigua.ai.tool.model.ToolResult;
import com.xigua.ai.utils.PromptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName RefineTool
 * @Description 优化工具调用结果，将工具原始输出交给LLM润色
 * @Author wangjinfei
 * @Date 2025/10/18 19:04
 */
@Slf4j
@Component
public class RefineTool implements AITool {
    @Autowired
    private LLMService llmService;

    @Override
    public String name() {
        return ToolType.REFINE.getName();
    }

    @Override
    public String description() {
        return "将工具调用的原始结果交给大模型，生成自然语言回答";
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        Map<String, Object> params = request.getParams();
        String userInput = (String) params.get("user_input");
        String toolName = (String) params.get("tool_name");
        String toolOutput = (String) params.get("tool_output");
        Boolean stream = (Boolean) params.get("stream");

        String refinePrompt = PromptUtil.getPrompt(Prompt.REFINE);
        refinePrompt = PromptUtil.fill(refinePrompt, Map.of(
                "user_input", userInput,
                "tool_name", toolName,
                "tool_output", toolOutput
        ));

        String refineInput = "这是工具调用后的结果，请根据它为用户生成自然语言回答：\n" + toolOutput;

        ChatContext chatContext = ChatContext.builder()
                .prompt(refinePrompt)
                .input(refineInput)
                .stream(stream)
                .build();

        try {
            String output = llmService.chat(chatContext).collectList()
                    .map(list -> String.join("", list))
                    .block();
            Map<String, Object> data = new HashMap<>();
            data.put("refined_output", output);
            return ToolResult.success(name(), output, null, data);
        } catch (Exception e) {
            log.error("调用模型失败, input: {}", userInput, e);
            return ToolResult.fail(name(), "调用模型失败");
        }
    }
}
