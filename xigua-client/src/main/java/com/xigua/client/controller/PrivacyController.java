package com.xigua.client.controller;

import com.xigua.client.service.PrivacyServiceImpl;
import com.xigua.common.core.base.R;
import com.xigua.domain.vo.LoginDeviceVO;
import com.xigua.domain.vo.PrivacySettingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "隐私与账号安全")
@RequestMapping("/privacy")
@RestController
public class PrivacyController {

    @Autowired
    private PrivacyServiceImpl privacyService;

    @Operation(summary = "读取隐私与通知偏好")
    @GetMapping("/settings")
    public R<PrivacySettingVO> settings() {
        return R.ok(privacyService.getSettings());
    }

    @Operation(summary = "保存隐私与通知偏好")
    @PutMapping("/settings")
    public R<PrivacySettingVO> saveSettings(@RequestBody PrivacySettingVO input) {
        return R.ok(privacyService.saveSettings(input), "已保存");
    }

    @Operation(summary = "黑名单列表")
    @GetMapping("/blocks")
    public R<List<Map<String, Object>>> blocks() {
        return R.ok(privacyService.blocks());
    }

    @Operation(summary = "加入黑名单")
    @PostMapping("/blocks")
    public R<Boolean> block(@RequestBody Map<String, String> body) {
        return R.ok(privacyService.block(body.get("targetId")), "已加入黑名单");
    }

    @Operation(summary = "移出黑名单")
    @DeleteMapping("/blocks/{targetId}")
    public R<Boolean> unblock(@PathVariable String targetId) {
        return R.ok(privacyService.unblock(targetId), "已移出黑名单");
    }

    @Operation(summary = "查询是否已拉黑")
    @GetMapping("/blocks/{targetId}/exists")
    public R<Boolean> blocked(@PathVariable String targetId) {
        return R.ok(privacyService.isBlocked(targetId));
    }

    @Operation(summary = "登录设备列表")
    @GetMapping("/devices")
    public R<List<LoginDeviceVO>> devices(HttpServletRequest request) {
        return R.ok(privacyService.devices(request));
    }

    @Operation(summary = "下线其它设备")
    @PostMapping("/devices/kick-others")
    public R<Integer> kickOthers(HttpServletRequest request) {
        return R.ok(privacyService.kickOtherDevices(request.getHeader("xigua-token")), "其它设备已下线");
    }

    @Operation(summary = "提交举报")
    @PostMapping("/report")
    public R<Boolean> report(@RequestBody Map<String, String> body) {
        return R.ok(privacyService.report(body.get("targetId"), body.get("targetType"),
                body.get("reason"), body.get("detail")), "举报已提交");
    }

    @Operation(summary = "导出个人数据")
    @GetMapping("/export")
    public R<Map<String, Object>> export() {
        return R.ok(privacyService.exportData(), "导出成功");
    }

    @Operation(summary = "注销账号")
    @PostMapping("/deactivate")
    public R<Boolean> deactivate(@RequestBody Map<String, String> body) {
        return R.ok(privacyService.deactivate(body.get("password"), body.get("confirmText")), "账号已注销");
    }
}
