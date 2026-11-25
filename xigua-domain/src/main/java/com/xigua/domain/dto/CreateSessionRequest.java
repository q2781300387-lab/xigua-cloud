package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @ClassName CreateSessionRequest
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/24 20:09
 */
@Data
@Schema(description = "创建AI会话请求")
public class CreateSessionRequest {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(description = "机器人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String botId;

    @Schema(description = "用户输入")
    private String input;

    @Schema(description = "模型名称（不传则用默认模型）")
    private String model;
}
