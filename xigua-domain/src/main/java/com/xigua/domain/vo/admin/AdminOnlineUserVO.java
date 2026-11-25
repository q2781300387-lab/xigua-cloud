package com.xigua.domain.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName AdminOnlineUserVO
 * @Description 管理端在线用户VO
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Data
@Schema(description = "管理端在线用户信息")
public class AdminOnlineUserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "登录IP")
    private String loginIp;

    @Schema(description = "登录地点")
    private String loginRegion;

    @Schema(description = "登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    @Schema(description = "服务器IP")
    private String host;

    @Schema(description = "服务器端口")
    private Integer port;

    @Schema(description = "User-Agent")
    private String ua;
}
