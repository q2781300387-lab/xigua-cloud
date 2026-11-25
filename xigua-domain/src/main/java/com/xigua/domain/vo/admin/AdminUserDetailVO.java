package com.xigua.domain.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName AdminUserDetailVO
 * @Description 管理端用户详情VO
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Data
@Schema(description = "管理端用户详情信息")
public class AdminUserDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "性别（1：男；2：女）")
    private Integer sex;

    @Schema(description = "地区")
    private String region;

    @Schema(description = "个性签名")
    private String signature;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "最后登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    @Schema(description = "账号状态（0：正常；1：禁言；2：封禁）")
    private Integer status;

    @Schema(description = "禁言截止时间；为空表示永久禁言")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime muteUntil;

    @Schema(description = "状态原因")
    private String statusReason;

    @Schema(description = "状态更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime statusUpdateTime;
}
