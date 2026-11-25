package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @ClassName MessageVO
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/24 20:15
 */
@Data
@Schema(description = "历史消息")
public class AiHistoryMessageVO {

    @Schema(description = "消息ID")
    private String messageId;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "消息时间（毫秒时间戳）")
    private Long timestamp;
}
