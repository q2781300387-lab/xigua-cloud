package com.xigua.center.controller;

import com.xigua.api.service.admin.NodeService;
import com.xigua.common.core.base.R;
import com.xigua.domain.dto.QueryNodePageDTO;
import com.xigua.domain.dto.SaveNodeDTO;
import com.xigua.domain.vo.admin.NodeDetailVO;
import com.xigua.domain.vo.admin.NodeMetricsVO;
import com.xigua.domain.vo.admin.NodeStatisticsVO;
import com.xigua.domain.vo.admin.NodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName NodeController
 * @Description 节点管理接口
 * @Author wangjinfei
 * @Date 2026/2/1 13:41
 */
@Tag(name = "节点管理接口")
@RequestMapping("/node")
@RestController
public class NodeController {

    @Autowired
    private NodeService nodeService;

    @Operation(summary = "分页查询节点", description = "根据节点名称、编码、地区等条件分页查询节点列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = NodeVO.class)))
    })
    @PostMapping("/page")
    public R page(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "分页查询参数", required = true)
                  @RequestBody QueryNodePageDTO dto) {
        return R.ok(nodeService.pageNodes(dto));
    }

    @Operation(summary = "查询节点详情", description = "根据节点ID查询节点详细信息（包含启动时间、运行状态等）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = NodeDetailVO.class)))
    })
    @GetMapping("/{id}")
    public R getDetail(@Parameter(description = "节点ID", required = true) @PathVariable("id") String id) {
        return R.ok(nodeService.getNodeDetail(id));
    }

    @Operation(summary = "保存节点", description = "新增或更新节点信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "保存成功")
    })
    @PostMapping
    public R save(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "节点信息", required = true)
                  @RequestBody SaveNodeDTO dto) {
        return R.ok(nodeService.saveNode(dto));
    }

    @Operation(summary = "更新节点", description = "根据节点ID更新节点信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    @PutMapping("/{id}")
    public R update(@Parameter(description = "节点ID", required = true) @PathVariable("id") String id,
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "节点信息", required = true)
                    @RequestBody SaveNodeDTO dto) {
        dto.setId(id);
        return R.ok(nodeService.saveNode(dto));
    }

    @Operation(summary = "批量删除节点", description = "根据节点ID列表批量删除节点")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功")
    })
    @DeleteMapping
    public R delete(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "节点ID列表", required = true)
                    @RequestBody List<String> ids) {
        return R.ok(nodeService.deleteNodes(ids));
    }

    @Operation(summary = "手动刷新节点系统信息", description = "手动触发指定节点的系统信息采集并更新到Redis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刷新成功")
    })
    @PostMapping("/refresh/{id}")
    public R refresh(@Parameter(description = "节点ID", required = true) @PathVariable("id") String id) {
        nodeService.refreshNodeSystem(id);
        return R.ok();
    }

    @Operation(summary = "获取节点统计信息", description = "获取总节点数、实时总连接数、在线节点数、离线节点数等统计信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = NodeStatisticsVO.class)))
    })
    @GetMapping("/statistics")
    public R getStatistics() {
        return R.ok(nodeService.getNodeStatistics());
    }

    @Operation(summary = "获取节点运行指标", description = "获取节点的连接数、TPS、CPU、内存等运行指标")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = NodeMetricsVO.class)))
    })
    @GetMapping("/{id}/metrics")
    public R getMetrics(@Parameter(description = "节点ID", required = true) @PathVariable("id") String id) {
        return R.ok(nodeService.getNodeMetrics(id));
    }

    @Operation(summary = "分页查询节点在线用户", description = "获取指定节点的在线用户列表，支持关键字搜索")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = com.xigua.domain.vo.admin.NodeOnlineUserVO.class)))
    })
    @PostMapping("/{id}/online-users")
    public R getOnlineUsers(@Parameter(description = "节点ID", required = true) @PathVariable("id") String id,
                            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "分页查询参数", required = true)
                            @RequestBody com.xigua.domain.dto.QueryNodeOnlineUserDTO dto) {
        dto.setNodeId(id);
        return R.ok(nodeService.pageNodeOnlineUsers(dto));
    }
}
