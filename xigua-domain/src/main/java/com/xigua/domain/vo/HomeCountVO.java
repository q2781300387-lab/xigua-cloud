package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName HomeCountVO
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/8/9 10:55
 */
@Data
public class HomeCountVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "好友数量")
    private Integer friendCount;

    @Schema(description = "群数量")
    private Integer groupCount;

    @Schema(description = "未读消息数量")
    private Long unreadCount;

    @Schema(description = "统计周期起始日，yyyy-MM-dd")
    private String weekStart;

    @Schema(description = "统计周期结束日，yyyy-MM-dd")
    private String weekEnd;

    @Schema(description = "本周已过天数，用于日均计算")
    private Integer elapsedDays;

    @Schema(description = "本周发送消息数")
    private Integer weekMessageCount;

    @Schema(description = "本周新增好友数")
    private Integer weekFriendCount;

    @Schema(description = "本周日均发送消息数，保留一位小数")
    private Double weekDailyAvg;

    @Schema(description = "消息环比百分比，null 表示缺少上周对比基准")
    private Double messageTrend;

    @Schema(description = "新增好友环比百分比，null 表示缺少上周对比基准")
    private Double friendTrend;

    @Schema(description = "日均环比百分比，null 表示缺少上周对比基准")
    private Double dailyTrend;
}
