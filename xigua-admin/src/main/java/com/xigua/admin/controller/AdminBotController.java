package com.xigua.admin.controller;

import com.xigua.api.service.admin.AdminBotService;
import com.xigua.common.core.base.R;
import com.xigua.domain.dto.QueryBotPageDTO;
import com.xigua.domain.vo.admin.AdminBotPageVO;
import com.xigua.domain.vo.admin.AdminBotDetailVO;
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
 * @ClassName AdminBotController
 * @Description 管理端机器人接口
 * @Author wangjinfei
 * @Date 2026/3/29
 */
@Tag(name = "管理端机器人接口", description = "提供管理端机器人管理相关功能")
@RequestMapping("/bot")
@RestController
public class AdminBotController {

    @Autowired
    private AdminBotService adminBotService;

    @Operation(summary = "分页查询机器人", description = "根据机器人名称等条件分页查询机器人列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AdminBotPageVO.class)))
    })
    @PostMapping("/page")
    public R page(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "分页查询参数", required = true)
                  @RequestBody QueryBotPageDTO dto) {
        return R.ok(adminBotService.page(dto));
    }

    @Operation(summary = "查询机器人详情", description = "根据机器人ID查询机器人详细信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AdminBotDetailVO.class)))
    })
    @GetMapping("/{botId}")
    public R getDetail(@Parameter(description = "机器人ID", required = true) @PathVariable("botId") String botId) {
        return R.ok(adminBotService.getDetail(botId));
    }
}
