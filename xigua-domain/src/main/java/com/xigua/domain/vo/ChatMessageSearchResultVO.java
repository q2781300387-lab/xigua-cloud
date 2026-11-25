package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName ChatMessageSearchResultVO
 * @Description 聊天记录搜索结果视图对象（在 ChatMessageSyncVO 基础上附加发送人/接收人头像昵称）
 * @Author wangjinfei
 * @Date 2026/9/29
 */
@Data
@Schema(title = "聊天记录搜索结果视图对象")
public class ChatMessageSearchResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息id")
    private String id;

    @Schema(description = "发送人id")
    private String senderId;

    @Schema(description = "发送人昵称/群名称/机器人名称")
    private String senderName;

    @Schema(description = "发送人头像")
    private String senderAvatar;

    @Schema(description = "接收人id（好友id/群id/机器人id）")
    private String receiverId;

    @Schema(description = "接收人昵称/群名称/机器人名称")
    private String receiverName;

    @Schema(description = "接收人头像")
    private String receiverAvatar;

    @Schema(description = "所属会话id（好友id/群id/机器人id）")
    private String conversationId;

    @Schema(description = "所属会话名称")
    private String conversationName;

    @Schema(description = "所属会话头像")
    private String conversationAvatar;

    @Schema(description = "所属会话类型（1：单聊；2：群聊；3：机器人）")
    private Integer conversationChatType;

    @Schema(description = "消息内容")
    private String message;

    @Schema(description = "命中关键字高亮后的消息内容（未命中高亮时与 message 相同）")
    private String highlightMessage;

    @Schema(description = "聊天类型（1：单聊；2：群聊；3：机器人）")
    private Integer chatType;

    @Schema(description = "消息类型（1文本 2图片 3语音 4视频 5文件）")
    private Integer messageType;

    @Schema(description = "创建时间")
    private String createTime;
}
