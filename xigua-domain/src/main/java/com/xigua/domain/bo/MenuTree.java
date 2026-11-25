package com.xigua.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName MenuTree
 * @Description 菜单树BO
 * @Author wangjinfei
 * @Date 2026/1/22 21:23
 */
@Data
@Schema(description = "菜单树")
public class MenuTree implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单ID")
    private String id;

    @Schema(description = "父菜单ID")
    private String parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单类型 (1-目录, 2-菜单, 3-按钮)")
    private Integer menuType;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "路由名称 (对应Vue组件name)")
    private String name;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "图标名称")
    private String icon;

    @Schema(description = "是否外链")
    private Boolean isExternal;

    @Schema(description = "是否缓存")
    private Boolean isCache;

    @Schema(description = "是否隐藏")
    private Boolean isHidden;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "菜单排序")
    private Integer menuSort;

    @Schema(description = "状态（0：禁用；1：启用）")
    private Integer status;

    @Schema(description = "子菜单列表")
    private List<MenuTree> children;
}
