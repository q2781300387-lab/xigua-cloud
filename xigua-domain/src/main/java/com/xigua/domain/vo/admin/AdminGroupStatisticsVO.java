package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName AdminGroupStatisticsVO
 * @Description 管理端群组统计信息
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Data
@Schema(description = "管理端群组统计信息")
public class AdminGroupStatisticsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "成员总数")
    private Long memberTotal;

    @Schema(description = "成员总数增长百分比")
    private Double memberGrowthRate;

    @Schema(description = "昨日活跃人数")
    private Long yesterdayActiveCount;

    @Schema(description = "昨日活跃人数增长百分比")
    private Double yesterdayActiveGrowthRate;

    @Schema(description = "累计消息量")
    private Long totalMessageCount;

    @Schema(description = "累计消息量增长百分比")
    private Double totalMessageGrowthRate;
}
