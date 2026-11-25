package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName SysRoleMenu
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/22 14:53
 */
@Data
@TableName("sys_role_menu")
@Schema(title = "菜单")
public class RoleMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键id */
    @TableId
    private String id;

    /** 角色id */
    private String roleId;

    /** 菜单id */
    private String menuId;

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
