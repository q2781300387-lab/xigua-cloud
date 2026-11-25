package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 编辑消息请求
 */
@Data
@Schema(title = "编辑消息请求")
public class EditChatMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息id")
    private String messageId;

    @Schema(description = "新的文本内容")
    private String content;
}
