package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName Group
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/7/5 17:11
 */
@Data
@TableName("xg_group")
@Schema(title = "群组实体")
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Schema(description = "主键id")
    @TableId(value = "id")
    private String id;

    /**
     * 群主id
     */
    @Schema(description = "群主id")
    private String ownerId;

    /**
     * 群名称
     */
    @Schema(description = "群名称")
    private String groupName;

    /**
     * 群头像
     */
    @Schema(description = "群头像")
    private String groupAvatar;

    /**
     * 群描述
     */
    @Schema(description = "群描述")
    private String description;

    /**
     * 群公告
     */
    @Schema(description = "群公告")
    private String announcement;

    /**
     * 公告更新时间
     */
    @Schema(description = "公告更新时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime announcementTime;

    /**
     * 公告发布者id
     */
    @Schema(description = "公告发布者id")
    private String announcementEditor;

    /**
     * 全员禁言（0：关闭；1：开启，仅群主与管理员可发言）
     */
    @Schema(description = "全员禁言（0：关闭；1：开启）")
    private Integer muteAll;

    /**
     * 当前成员数
     */
    @Schema(description = "当前成员数")
    private Integer currentMember;

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
