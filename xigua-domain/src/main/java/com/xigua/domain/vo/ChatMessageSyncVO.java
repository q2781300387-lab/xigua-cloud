package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName ChatMessageSyncVO
 * @Description 聊天消息同步视图对象（用于 MySQL -> ES 全量同步，不携带 mybatis-plus 注解）
 * @Author wangjinfei
 * @Date 2026/9/29
 */
@Data
@Schema(title = "聊天消息同步视图对象")
public class ChatMessageSyncVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息id")
    private String id;

    @Schema(description = "发送人id")
    private String senderId;

    @Schema(description = "接收人id（好友id/群id/机器人id）")
    private String receiverId;

    @Schema(description = "消息内容")
    private String message;

    /**
     * 与 message 同值的精确匹配副本，对应 ES 里 keyword 类型的 messageRaw 字段。
     * smartcn 会把 C38INCR 这类英文数字串切成 C/38/INCR，检索时用整串走 term 查询兜底。
     */
    @Schema(description = "消息内容原文（用于精确匹配）")
    private String messageRaw;

    @Schema(description = "聊天类型（1：单聊；2：群聊；3：机器人）")
    private Integer chatType;

    @Schema(description = "消息类型（1文本 2图片 3语音 4视频 5文件）")
    private Integer messageType;

    @Schema(description = "是否撤回（0：未撤回；1：已撤回）")
    private Integer withdrawn;

    @Schema(description = "引用的消息id")
    private String replyToId;

    @Schema(description = "引用消息内容快照")
    private String replyContent;

    @Schema(description = "@提及的用户id列表（JSON数组字符串）")
    private String atUsers;

    @Schema(description = "创建时间")
    private String createTime;
}
