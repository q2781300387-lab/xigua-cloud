package com.xigua.admin.controller;

import com.xigua.api.service.admin.AdminStatisticsService;
import com.xigua.common.core.base.R;
import com.xigua.domain.vo.admin.ActiveGroupRankVO;
import com.xigua.domain.vo.admin.AdminHomeStatisticsVO;
import com.xigua.domain.vo.admin.MessageTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName AdminHomeController
 * @Description 管理端首页接口
 * @Author wangjinfei
 * @Date 2026/3/9
 */
@Tag(name = "管理端首页接口", description = "提供管理端首页相关功能")
@RequestMapping("/home")
@RestController
public class AdminHomeController {
    @Autowired
    private AdminStatisticsService adminStatisticsService;

    @Operation(summary = "获取首页统计信息",
            description = "获取总注册用户、实时在线数、今日消息总量、活跃群组数及其增长率")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AdminHomeStatisticsVO.class)))
    })
    @GetMapping("/statistics")
    public R<AdminHomeStatisticsVO> getHomeStatistics() {
        return R.ok(adminStatisticsService.getHomeStatistics());
    }

    @Operation(summary = "获取消息发送趋势",
            description = "获取最近24小时的消息发送趋势数据，前端每5分钟调用一次")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = MessageTrendVO.class)))
    })
    @GetMapping("/message-trend")
    public R<MessageTrendVO> getMessageTrend() {
        return R.ok(adminStatisticsService.getMessageTrend());
    }

    @Operation(summary = "获取活跃群组排行",
            description = "获取按成员数排序的前5个活跃群组，前端每5分钟调用一次")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ActiveGroupRankVO.class)))
    })
    @GetMapping("/active-group-rank")
    public R<ActiveGroupRankVO> getActiveGroupRank() {
        return R.ok(adminStatisticsService.getActiveGroupRank());
    }
}
