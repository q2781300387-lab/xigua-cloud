package com.xigua.admin.controller;

import com.xigua.api.service.admin.MiddlewareMonitorService;
import com.xigua.common.core.base.R;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.dto.MiddlewareQueryDTO;
import com.xigua.domain.vo.admin.MiddlewareStatsVO;
import com.xigua.domain.vo.admin.MiddlewarePageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName MiddlewareMonitorController
 * @Description 中间件监控接口
 * @Author Claude Code
 * @Date 2026/5/8
 */
@Tag(name = "中间件监控", description = "提供中间件监控相关功能")
@RequestMapping("/middleware/monitor")
@RestController
public class MiddlewareMonitorController {

    @Autowired
    private MiddlewareMonitorService middlewareMonitorService;

    @Operation(summary = "获取中间件统计卡片数据",
            description = "获取实例数量、服务健康率、累计连接量、平均资源使用率")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = MiddlewareStatsVO.class)))
    })
    @GetMapping("/stats")
    public R<MiddlewareStatsVO> getMiddlewareStats() {
        return R.ok(middlewareMonitorService.getMiddlewareStats());
    }

    @Operation(summary = "分页查询中间件列表",
            description = "分页查询中间件实例列表，支持按组件名称、环境、状态筛选")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = BasePageVO.class)))
    })
    @PostMapping("/page")
    public R<BasePageVO<MiddlewarePageVO>> getMiddlewarePage(@RequestBody MiddlewareQueryDTO queryDTO) {
        return R.ok(middlewareMonitorService.getMiddlewarePage(queryDTO));
    }
}
