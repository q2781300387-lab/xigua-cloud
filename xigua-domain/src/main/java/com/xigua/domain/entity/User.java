package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName User
 * @Description
 * @Author wangjinfei
 * @Date 2025/4/27 8:38
 */
@Data
@TableName("xg_user")
@Schema(title = "用户实体")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键id
     */
    @Schema(description = "主键id")
    @TableId(value = "id")
    private String id;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 密码：只允许反序列化写入，任何接口响应都不再返回密码哈希
     */
    @Schema(description = "密码")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 头像
     */
    @Schema(description = "头像")
    private String avatar;

    /**
     * 性别（1；男；2：女）
     */
    @Schema(description = "性别（1；男；2：女）")
    private Integer sex;

    /**
     * 地区
     */
    @Schema(description = "地区")
    private String region;

    /**
     * 个性签名
     */
    @Schema(description = "个性签名")
    private String signature;

    /**
     * 是否管理员（0：否；1：是）
     */
    @Schema(description = "是否管理员（0：否；1：是）")
    private Integer isAdmin;

    /**
     * 账号状态（0：正常；1：禁言；2：封禁）
     */
    @Schema(description = "账号状态（0：正常；1：禁言；2：封禁）")
    private Integer status;

    /**
     * 禁言截止时间；为空表示永久禁言
     */
    @Schema(description = "禁言截止时间；为空表示永久禁言")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime muteUntil;

    /**
     * 状态原因
     */
    @Schema(description = "状态原因")
    private String statusReason;

    /**
     * 状态更新时间
     */
    @Schema(description = "状态更新时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime statusUpdateTime;

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
