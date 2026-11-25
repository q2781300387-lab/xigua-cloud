package com.xigua.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName NodeSystemInfoDTO
 * @Description 节点系统信息DTO
 * @Author wangjinfei
 * @Date 2026/3/18
 */
@Data
@Schema(description = "节点系统信息")
public class NodeSystemInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "节点IP(内网ip)")
    private String nodeIp;

    @Schema(description = "CPU使用率（百分比）")
    private Double cpuUsage;

    @Schema(description = "内存使用率（百分比）")
    private Double memoryUsage;

    @Schema(description = "JVM内存使用率（百分比）")
    private Double jvmMemoryUsage;

    @Schema(description = "CPU核心数")
    private Integer cpuCores;

    @Schema(description = "总内存大小（字节）")
    private Long totalMemory;

    @Schema(description = "操作系统信息")
    private String osInfo;

    @Schema(description = "CPU信息")
    private String cpuInfo;

    @Schema(description = "内存详情")
    private String memoryInfo;

    @Schema(description = "JVM内存详情")
    private String jvmMemoryInfo;

    @Schema(description = "网络延迟（毫秒），-1表示获取失败")
    private Long networkLatency;

    @Schema(description = "更新时间戳")
    private Long timestamp;
}
