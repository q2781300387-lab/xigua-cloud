package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName AiSession
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/24 21:06
 */
@Data
@TableName("xg_ai_session")
@Schema(title = "AI会话")
public class AiSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键id */
    @TableId
    @Schema(description = "主键id")
    private String id;

    /** 机器人id */
    @Schema(description = "机器人id")
    private String botId;

    /** 用户id */
    @Schema(description = "用户id")
    private String userId;

    /** 模型 */
    @Schema(description = "模型")
    private String model;

    /** 会话标题 */
    @Schema(description = "会话标题")
    private String title;

    /** 系统提示词 */
    @Schema(description = "系统提示词")
    private String systemPrompt;

    /** 消息数量 */
    @Schema(description = "消息数量")
    private Integer messageCount;

    /** 总token消耗 */
    @Schema(description = "总token消耗")
    private Long totalTokens;

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
