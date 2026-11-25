package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName UserResourceQuota
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/27 9:57
 */
@Data
@TableName("xg_user_resource_quota")
@Schema(title = "用户资源配额统计表")
public class UserResourceQuota implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键id
     */
    @Schema(description = "主键id")
    @TableId(value = "id")
    private String id;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private String userId;

    /**
     * 已使用总容量（字节）
     */
    @Schema(description = "已使用总容量（字节）")
    private Long usedSize;

    /**
     * 用户名
     */
    @Schema(description = "已使用资源数量")
    private Integer usedCount;

    /**
     * 最大允许总容量（字节）
     */
    @Schema(description = "最大允许总容量（字节）")
    private Long maxTotalSize;

    /**
     * 单个资源最大大小（字节）
     */
    @Schema(description = "单个资源最大大小（字节）")
    private Long maxFileSize;

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
