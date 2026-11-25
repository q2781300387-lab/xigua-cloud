package com.xigua.ai.agent.model;

import com.xigua.ai.llm.model.Memory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName AgentContext
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/10/18 19:06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {
    private String requestId;
    private String input;
    private Boolean stream;
    private String systemPrompt;
    private List<Memory> memory;
    /** 会话选择的模型名称（为空使用全局默认模型） */
    private String model;
}
