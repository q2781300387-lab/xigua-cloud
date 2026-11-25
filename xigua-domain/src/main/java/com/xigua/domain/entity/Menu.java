package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName SysMenu
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/22 14:52
 */
@Data
@TableName("sys_menu")
@Schema(title = "菜单")
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键id */
    @TableId
    private String id;

    /** 菜单名称 */
    private String menuName;

    /** 父菜单id */
    private Long parentId;

    /** 排序 */
    private Integer menuSort;

    /** 路由路径 */
    private String path;

    /** 组件 */
    private String component;

    /** 菜单类型（1：目录；2：菜单；3：按钮） */
    private Integer menuType;

    /** 状态（0：禁用；1：启用） */
    private Integer status;

    /** 权限标识 */
    private String perms;

    /** 图标 */
    private String icon;

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
