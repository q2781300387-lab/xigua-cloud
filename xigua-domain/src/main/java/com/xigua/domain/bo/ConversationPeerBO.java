package com.xigua.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话列表回源用：每个会话（单聊对端 / 群 / 机器人）的最新一条消息
 * 只承载重建 Redis 缓存所需的最小字段，主体信息仍由 resolveChatMeta 补齐
 */
@Data
@Schema(description = "会话最新一条消息")
public class ConversationPeerBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话对端：单聊是好友id，群聊是群id，机器人是机器人id
     */
    @Schema(description = "会话对端id")
    private String peerId;

    /**
     * 聊天类型（1：单聊；2：群聊；3：机器人）
     */
    @Schema(description = "聊天类型（1：单聊；2：群聊；3：机器人）")
    private Integer chatType;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容")
    private String message;

    /**
     * 消息时间
     */
    @Schema(description = "消息时间")
    private LocalDateTime createTime;
}
