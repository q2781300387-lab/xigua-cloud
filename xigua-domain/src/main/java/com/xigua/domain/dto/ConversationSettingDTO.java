package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 会话设置请求
 */
@Data
@Schema(title = "会话设置请求")
public class ConversationSettingDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "会话id：单聊为好友id，群聊为群id")
    private String chatId;

    @Schema(description = "会话类型：1单聊 2群聊 3机器人")
    private Integer chatType;

    @Schema(description = "是否置顶；不传表示不修改")
    private Boolean pinned;

    @Schema(description = "草稿；不传表示不修改，传空字符串表示清空")
    private String draft;
}
