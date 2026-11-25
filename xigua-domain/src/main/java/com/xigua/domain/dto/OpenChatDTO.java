package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName OpenChatDTO
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/12/6 18:17
 */
@Data
@Schema(description = "打开聊天")
public class OpenChatDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "聊天ID")
    private String chatId;
    @Schema(description = "聊天类型（1：单聊；2：群聊；3：机器人）")
    private Integer chatType;
}
