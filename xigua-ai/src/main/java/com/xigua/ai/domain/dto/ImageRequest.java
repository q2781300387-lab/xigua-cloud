package com.xigua.ai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 绘画请求参数
 */
@Data
@Schema(description = "AI绘画请求参数")
public class ImageRequest {
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(description = "机器人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String botId;

    @Schema(description = "会话id（可选，传入则图片消息挂到该会话）")
    private String sessionId;

    @Schema(description = "绘画描述", requiredMode = Schema.RequiredMode.REQUIRED)
    private String prompt;
}
