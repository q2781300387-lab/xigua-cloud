package com.xigua.ai.llm.model;

import com.xigua.ai.sse.StreamCallback;
import com.xigua.domain.bo.AiMemoryBO;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName ChatContext
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/9/23 20:07
 */
@Data
@Builder
public class ChatContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private String prompt;
    private String input;
    private List<Memory> memory;
    private Boolean stream;
    /** 会话选择的模型名称（为空使用全局默认模型） */
    private String model;
}
