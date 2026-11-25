package com.xigua.admin.controller;

import com.xigua.api.service.admin.AdminLoginLogService;
import com.xigua.common.core.base.R;
import com.xigua.domain.dto.QueryLoginLogPageDTO;
import com.xigua.domain.vo.admin.AdminLoginLogPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName AdminLoginLogController
 * @Description 管理端登录日志接口
 * @Author wangjinfei
 * @Date 2026/3/8
 */
@Tag(name = "管理端登录日志接口", description = "提供管理端登录日志查询功能")
@RequestMapping("/login-log")
@RestController
public class AdminLoginLogController {

    @Autowired
    private AdminLoginLogService adminLoginLogService;

    @Operation(summary = "分页查询登录日志", description = "根据用户名、操作类型、登录状态、创建时间等条件分页查询登录日志列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AdminLoginLogPageVO.class)))
    })
    @PostMapping("/page")
    public R page(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "分页查询参数", required = true) @RequestBody QueryLoginLogPageDTO dto) {
        return R.ok(adminLoginLogService.page(dto));
    }
}
