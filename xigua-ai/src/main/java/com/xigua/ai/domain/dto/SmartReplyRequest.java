package com.xigua.ai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @ClassName SmartReplyRequest
 * @Description AI 智能回复请求参数
 * @Author xigua
 * @Date 2026/10/16 19:30
 */
@Data
@Schema(description = "AI 智能回复请求参数")
public class SmartReplyRequest {

    @Schema(description = "对方发来的消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "可选场景：single/group/moments，默认 single")
    private String scene;
}
