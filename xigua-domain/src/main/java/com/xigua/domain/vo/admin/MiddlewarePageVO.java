package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName MiddlewareVO
 * @Description 中间件信息
 * @Author Claude Code
 * @Date 2026/5/8
 */
@Data
@Schema(description = "中间件信息")
public class MiddlewarePageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "容器ID")
    private String containerId;

    @Schema(description = "组件名称")
    private String componentName;

    @Schema(description = "运行状态", example = "在线/离线")
    private String status;

    @Schema(description = "CPU使用率(%)")
    private Double cpuUsage;

    @Schema(description = "内存使用量(GB)")
    private Double memoryUsed;

    @Schema(description = "内存总量(GB)")
    private Double memoryTotal;

    @Schema(description = "资源使用率显示文本", example = "45% / 2.4GB")
    private String resourceUsageText;

    @Schema(description = "运行时间(秒)")
    private Long uptime;

    @Schema(description = "运行时间显示文本", example = "14d 2h 45m")
    private String uptimeText;

    @Schema(description = "端口映射")
    private String ports;

    @Schema(description = "镜像名称")
    private String image;

    @Schema(description = "创建时间")
    private Long createdAt;

    @Schema(description = "运行开始时间（时间戳毫秒）")
    private Long startedAt;
}
