package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @ClassName ActivityStatsVO
 * @Description 活跃统计响应结果
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "活跃统计响应结果")
public class ActivityStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 已发送消息数
     */
    @Schema(description = "已发送消息数")
    private Long sentMessages;

    /**
     * 消息增长趋势（百分比）
     */
    @Schema(description = "消息增长趋势（百分比）")
    private Double messageTrend;

    /**
     * 活跃群组数
     */
    @Schema(description = "活跃群组数")
    private Long activeGroups;

    /**
     * 群组增长趋势（百分比）
     */
    @Schema(description = "群组增长趋势（百分比）")
    private Double groupTrend;

    /**
     * 好友总数
     */
    @Schema(description = "好友总数")
    private Long totalFriends;

    /**
     * 好友增长趋势（百分比）
     */
    @Schema(description = "好友增长趋势（百分比）")
    private Double friendTrend;
}
