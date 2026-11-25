package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName LoginLog
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/15 9:50
 */
@Data
@TableName("xg_login_log")
@Schema(title = "登录实体")
public class LoginLog implements Serializable {
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
     * 登录方式（0：登出；1：密码）
     */
    @Schema(description = "登录方式（0：登出；1：密码）")
    private Integer loginType;

    /**
     * 登录ip
     */
    @Schema(description = "登录ip")
    private String loginIp;

    /**
     * 登录区域
     */
    @Schema(description = "登录区域")
    private String loginRegion;

    /**
     * ua
     */
    @Schema(description = "ua")
    private String userAgent;

    /**
     * 平台（1：web；2：app；3：mini）
     */
    @Schema(description = "平台（1：web；2：app；3：mini）")
    private Integer platform;

    /**
     * 登录状态（登陆状态：1：成功；0：失败）
     */
    @Schema(description = "登录状态（登陆状态：1：成功；0：失败）")
    private Integer loginStatus;

    /**
     * 登录消息
     */
    @Schema(description = "登录消息")
    private String loginMsg;

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
}
