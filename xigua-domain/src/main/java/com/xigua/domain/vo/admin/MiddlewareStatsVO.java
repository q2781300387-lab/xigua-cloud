package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName MiddlewareStatsVO
 * @Description 中间件统计卡片数据
 * @Author Claude Code
 * @Date 2026/5/8
 */
@Data
@Schema(description = "中间件统计卡片数据")
public class MiddlewareStatsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "实例数量")
    private Integer instanceCount;

    @Schema(description = "服务健康率(%)")
    private Double healthRate;

    @Schema(description = "累计连接量")
    private Long totalConnections;

    @Schema(description = "平均资源使用率(%)")
    private Double avgResourceUsage;
}
