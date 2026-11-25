package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName NodeLog
 * @Description 节点日志
 * @Author Claude
 * @Date 2026/03/18
 */
@Data
@TableName("xg_node_log")
@Schema(description = "节点日志")
public class NodeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Schema(description = "主键id")
    @TableId
    private String id;

    /**
     * 节点id
     */
    @Schema(description = "节点id")
    private String nodeId;

    /**
     * 启动时间
     */
    @Schema(description = "启动时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 下线时间
     */
    @Schema(description = "下线时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 状态（0：下线；1：在线）
     */
    @Schema(description = "状态（0：下线；1：在线）")
    private Integer status;

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
