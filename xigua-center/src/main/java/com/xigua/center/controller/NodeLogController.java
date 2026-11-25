package com.xigua.center.controller;

import com.xigua.api.service.admin.NodeLogService;
import com.xigua.common.core.base.R;
import com.xigua.domain.dto.QueryNodeLogPageDTO;
import com.xigua.domain.vo.admin.NodeLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName NodeLogController
 * @Description 节点日志管理接口
 * @Author Claude
 * @Date 2026/03/18
 */
@Tag(name = "节点日志管理接口")
@RequestMapping("/node-log")
@RestController
public class NodeLogController {

    @Autowired
    private NodeLogService nodeLogService;

    @Operation(summary = "分页查询节点日志", description = "根据节点ID、状态、时间范围等条件分页查询节点日志列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = NodeLogVO.class)))
    })
    @PostMapping("/page")
    public R page(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "分页查询参数", required = true)
                  @RequestBody QueryNodeLogPageDTO dto) {
        return R.ok(nodeLogService.pageNodeLogs(dto));
    }

    @Operation(summary = "查询节点日志详情", description = "根据日志ID查询节点日志详细信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = NodeLogVO.class)))
    })
    @GetMapping("/{id}")
    public R getDetail(@Parameter(description = "日志ID", required = true) @PathVariable("id") String id) {
        return R.ok(nodeLogService.getNodeLogById(id));
    }
}
