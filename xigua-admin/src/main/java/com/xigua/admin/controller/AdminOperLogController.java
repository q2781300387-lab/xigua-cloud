package com.xigua.admin.controller;

import com.xigua.admin.domain.OperLog;
import com.xigua.admin.dto.OperLogPageQuery;
import com.xigua.admin.service.AdminOperLogService;
import com.xigua.common.core.base.R;
import com.xigua.common.mybatis.base.BasePageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 后台操作审计日志查询。
 */
@Tag(name = "管理端操作日志接口", description = "提供后台写操作审计日志查询")
@RequestMapping("/oper-log")
@RestController
public class AdminOperLogController {

    private final AdminOperLogService adminOperLogService;

    public AdminOperLogController(AdminOperLogService adminOperLogService) {
        this.adminOperLogService = adminOperLogService;
    }

    @Operation(summary = "分页查询操作日志")
    @PostMapping("/page")
    public R<BasePageVO<OperLog>> page(@RequestBody OperLogPageQuery query) {
        return R.ok(adminOperLogService.page(query));
    }

    @Operation(summary = "操作日志概览")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(OperLogPageQuery query) {
        return R.ok(adminOperLogService.stats(query));
    }

    @Operation(summary = "操作日志模块列表")
    @GetMapping("/modules")
    public R<List<String>> modules() {
        return R.ok(adminOperLogService.modules());
    }
}
