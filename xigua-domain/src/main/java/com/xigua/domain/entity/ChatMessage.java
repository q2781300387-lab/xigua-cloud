package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName Message
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/17 19:12
 */
@Data
@TableName("xg_chat_message")
@Schema(title = "消息实体")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键id
     */
    @Schema(description = "主键id")
    @TableId(value = "id")
    private String id;

    /**
     * 客户端消息id（幂等键）
     */
    @Schema(description = "客户端消息id")
    private String clientMsgId;

    /**
     * 发起人
     */
    @Schema(description = "发起人")
    private String senderId;

    /**
     * 接收人
     */
    @Schema(description = "接收人")
    private String receiverId;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容")
    private String message;

    /**
     * 聊天类型（1：单聊；2：群聊）
     */
    @Schema(description = "聊天类型（1：单聊；2：群聊）")
    private Integer chatType;

    /**
     * 消息类型（1文本 2图片 3语音 4视频 5文件）
     */
    @Schema(description = "消息类型（1文本 2图片 3语音 4视频 5文件）")
    private Integer messageType;

    /**
     * 纬度
     */
    @Schema(description = "纬度")
    private String lat;

    /**
     * 经度
     */
    @Schema(description = "经度")
    private String lng;

    /**
     * ip地址
     */
    @Schema(description = "ip地址")
    private String ip;

    /**
     * 是否撤回 （0：未撤回；1：已撤回）
     */
    @Schema(description = "是否撤回 （0：未撤回；1：已撤回）")
    private Integer withdrawn;

    /**
     * 引用的消息id
     */
    @Schema(description = "引用的消息id")
    private String replyToId;

    /**
     * 引用消息内容快照（防止原消息撤回后引用失效）
     */
    @Schema(description = "引用消息内容快照")
    private String replyContent;

    /**
     * @提及的用户id列表（JSON数组字符串，如 ["10001","10002"]）
     */
    @Schema(description = "@提及的用户id列表")
    private String atUsers;

    /**
     * 是否编辑过（0：未编辑；1：已编辑）
     */
    @Schema(description = "是否编辑过 （0：未编辑；1：已编辑）")
    private Integer edited;

    /**
     * 最后编辑时间
     */
    @Schema(description = "最后编辑时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime editTime;

    /**
     * 是否删除 （0：未删除；1：已删除）
     */
    @Schema(description = "是否删除 （0：未删除；1：已删除）")
    @TableLogic
    private Integer delFlag;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 修改人
     */
    @Schema(description = "修改人")
    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
