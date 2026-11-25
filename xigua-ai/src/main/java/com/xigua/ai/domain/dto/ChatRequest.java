package com.xigua.ai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @ClassName ChatRequest
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/24 20:13
 */
@Data
@Schema(description = "聊天请求参数")
public class ChatRequest {
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(description = "机器人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String botId;

    @Schema(description = "用户会话id")
    private String sessionId;

    @Schema(description = "用户输入内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String input;

    @Schema(description = "是否流式输出", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean stream;
}
