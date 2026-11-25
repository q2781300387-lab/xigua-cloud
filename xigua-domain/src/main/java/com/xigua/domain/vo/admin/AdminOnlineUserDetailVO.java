package com.xigua.domain.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName AdminOnlineUserDetailVO
 * @Description 在线用户详情VO
 * @Author Claude
 * @Date 2026/03/20
 */
@Data
@Schema(description = "在线用户详情信息")
public class AdminOnlineUserDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 设备信息 ==========
    @Schema(description = "设备类型（Mobile：移动端；Web：Web端）")
    private String deviceType;

    @Schema(description = "设备信息（如：iPhone 15 Pro 或 Chrome）")
    private String device;

    @Schema(description = "系统版本（如：iOS 17.4 或浏览器信息）")
    private String osVersion;

    @Schema(description = "客户端版本")
    private String clientVersion;

    // ========== 连接信息 ==========
    @Schema(description = "登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    @Schema(description = "在线时长（格式化，如：02:15:30）")
    private String onlineDuration;

    @Schema(description = "心跳状态")
    private String heartbeatStatus;

    // ========== 网络信息 ==========
    @Schema(description = "IP 地址")
    private String ipAddress;

    @Schema(description = "登录地点")
    private String loginLocation;

    @Schema(description = "经度")
    private String lng;

    @Schema(description = "纬度")
    private String lat;
}
