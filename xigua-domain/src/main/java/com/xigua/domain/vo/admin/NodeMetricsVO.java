package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName NodeMetricsVO
 * @Description 节点运行指标VO
 * @Author Claude
 * @Date 2026/03/19
 */
@Data
@Schema(description = "节点运行指标信息")
public class NodeMetricsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "连接人数")
    private Integer connectionCount;

    @Schema(description = "消息吞吐（TPS）")
    private Integer tps;

    @Schema(description = "CPU使用率（百分比）")
    private Double cpuUsage;

    @Schema(description = "内存使用率（百分比）")
    private Double memoryUsage;
}
