package com.xigua.domain.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName AdminUserPage
 * @Description 管理端用户分页VO
 * @Author wangjinfei
 * @Date 2026/2/1 17:08
 */
@Data
@Schema(description = "管理端用户分页信息")
public class AdminUserPage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别 (0-女, 1-男, 2-未知)")
    private Integer sex;

    @Schema(description = "账号状态（0：正常；1：禁言；2：封禁）")
    private Integer status;

    @Schema(description = "禁言截止时间；为空表示永久禁言")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime muteUntil;

    @Schema(description = "状态原因")
    private String statusReason;

    @Schema(description = "创建时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
