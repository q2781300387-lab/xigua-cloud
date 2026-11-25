package com.xigua.admin.controller;

import com.xigua.admin.domain.ConfigItem;
import com.xigua.admin.dto.ConfigPageQuery;
import com.xigua.admin.dto.ConfigSaveDTO;
import com.xigua.admin.service.AdminConfigService;
import com.xigua.common.core.base.R;
import com.xigua.common.mybatis.base.BasePageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 全局配置管理。
 */
@Tag(name = "管理端全局配置接口", description = "提供配置项查询、新增、修改与删除")
@RequestMapping("/config")
@RestController
public class AdminConfigController {

    private final AdminConfigService adminConfigService;

    public AdminConfigController(AdminConfigService adminConfigService) {
        this.adminConfigService = adminConfigService;
    }

    @Operation(summary = "分页查询配置项")
    @GetMapping("/page")
    public R<BasePageVO<ConfigItem>> page(ConfigPageQuery query) {
        return R.ok(adminConfigService.page(query));
    }

    @Operation(summary = "配置分组列表")
    @GetMapping("/groups")
    public R<List<String>> groups() {
        return R.ok(adminConfigService.groups());
    }

    @Operation(summary = "配置概览")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(adminConfigService.stats());
    }

    @Operation(summary = "新增配置项")
    @PostMapping
    public R<ConfigItem> create(@RequestBody ConfigSaveDTO dto) {
        return R.ok(adminConfigService.create(dto), "配置项已创建");
    }

    @Operation(summary = "修改配置项")
    @PutMapping("/{id}")
    public R<ConfigItem> update(@PathVariable Long id, @RequestBody ConfigSaveDTO dto) {
        dto.setId(id);
        return R.ok(adminConfigService.update(dto), "配置项已保存");
    }

    @Operation(summary = "删除配置项")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminConfigService.delete(id);
        return R.ok(null, "配置项已删除");
    }
}
