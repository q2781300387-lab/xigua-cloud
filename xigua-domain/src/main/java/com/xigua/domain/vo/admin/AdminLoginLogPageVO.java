package com.xigua.domain.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName AdminLoginLogPageVO
 * @Description 管理端登录日志分页VO
 * @Author wangjinfei
 * @Date 2026/3/8
 */
@Data
@Schema(description = "管理端登录日志分页信息")
public class AdminLoginLogPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "操作（0：登出；1：密码登录）")
    private Integer loginType;

    @Schema(description = "操作描述")
    private String loginTypeDesc;

    @Schema(description = "登录状态（1：成功；0：失败）")
    private Integer loginStatus;

    @Schema(description = "状态描述")
    private String loginStatusDesc;

    @Schema(description = "登录IP（已脱敏）")
    private String loginIp;

    @Schema(description = "地理位置")
    private String loginRegion;

    @Schema(description = "设备/浏览器（UA）")
    private String userAgent;
}
