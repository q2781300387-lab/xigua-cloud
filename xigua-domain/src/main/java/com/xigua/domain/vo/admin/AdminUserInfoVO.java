package com.xigua.domain.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @ClassName AdminUserInfoVO
 * @Description 管理端用户信息VO
 * @Author wangjinfei
 * @Date 2026/1/22 20:08
 */
@Data
@Schema(description = "管理端用户信息")
public class AdminUserInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "性别 (0-女, 1-男, 2-未知)")
    private Integer gender;

    @Schema(description = "邮箱 (已脱敏)")
    private String email;

    @Schema(description = "手机号 (已脱敏)")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "描述/备注")
    private String description;

    @Schema(description = "密码重置时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pwdResetTime;

    @Schema(description = "密码是否已过期")
    @JsonProperty("pwdExpired")
    private Boolean pwdExpired;

    @Schema(description = "注册日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate registrationDate;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "权限列表")
    private List<String> permissions;

    @Schema(description = "角色编码列表")
    private List<String> roles;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;
}
