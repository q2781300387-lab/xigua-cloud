package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName AdminHomeStatisticsVO
 * @Description 管理端首页统计信息
 * @Author wangjinfei
 * @Date 2026/3/9
 */
@Data
@Schema(description = "管理端首页统计信息")
public class AdminHomeStatisticsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "总注册用户数")
    private Long totalUserCount;

    @Schema(description = "总注册用户增长百分比")
    private Double totalUserGrowthRate;

    @Schema(description = "实时在线用户数")
    private Long onlineUserCount;

    @Schema(description = "实时在线用户增长百分比")
    private Double onlineUserGrowthRate;

    @Schema(description = "今日消息总量")
    private Long todayMessageCount;

    @Schema(description = "今日消息总量增长百分比")
    private Double todayMessageGrowthRate;

    @Schema(description = "活跃群组数（近7天有消息）")
    private Long activeGroupCount;

    @Schema(description = "活跃群组数增长百分比")
    private Double activeGroupGrowthRate;
}
