package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 会话个性化设置（置顶 / 草稿）
 */
@Data
@Schema(description = "会话设置")
public class ConversationSettingVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "会话id：单聊为好友id，群聊为群id")
    private String chatId;

    @Schema(description = "会话类型：1单聊 2群聊 3机器人")
    private Integer chatType;

    @Schema(description = "是否置顶")
    private Boolean pinned;

    @Schema(description = "草稿内容")
    private String draft;
}
