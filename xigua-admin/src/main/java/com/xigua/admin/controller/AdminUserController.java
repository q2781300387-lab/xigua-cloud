package com.xigua.admin.controller;

import com.xigua.api.service.admin.AdminUserService;
import com.xigua.domain.dto.QueryUserPageDTO;
import com.xigua.domain.dto.UpdateUserDTO;
import com.xigua.domain.dto.admin.UserModerationDTO;
import com.xigua.common.core.base.R;
import com.xigua.domain.vo.admin.ActivityStatsVO;
import com.xigua.domain.vo.admin.AdminUserDetailVO;
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
 * @ClassName UserController
 * @Description 管理端用户接口
 * @Author wangjinfei
 * @Date 2026/2/1 17:04
 */
@Tag(name = "管理端用户接口", description = "提供管理端用户管理相关功能")
@RequestMapping("/user")
@RestController
public class AdminUserController {
    @Autowired
    private AdminUserService adminUserService;

    @Operation(summary = "分页查询用户", description = "根据用户名等条件分页查询管理端用户列表")
    @PostMapping("/page")
    public R userInfo(@RequestBody QueryUserPageDTO dto){
        return R.ok(adminUserService.page(dto));
    }

    @Operation(summary = "查询用户详情", description = "根据用户ID查询用户详细信息")
    @GetMapping("/{userId}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AdminUserDetailVO.class)))
    })
    public R getDetail(@Parameter(description = "用户ID", required = true) @PathVariable("userId") String userId){
        return R.ok(adminUserService.getDetail(userId));
    }

    @Operation(summary = "获取活跃统计", description = "根据用户ID获取已发送消息数、活跃群组数、好友总数等统计数据")
    @GetMapping("/stats/activity/{userId}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ActivityStatsVO.class)))
    })
    public R getActivityStats(@Parameter(description = "用户ID", required = true) @PathVariable("userId") String userId){
        return R.ok(adminUserService.getActivityStats(userId));
    }

    @Operation(summary = "更新用户信息", description = "根据用户ID更新用户信息（用户名、邮箱、手机号、头像、性别、地区、个性签名）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    @PutMapping("/{userId}")
    public R updateUser(@Parameter(description = "用户ID", required = true) @PathVariable("userId") String userId,
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户更新信息", required = true)
                        @RequestBody UpdateUserDTO dto){
        dto.setId(userId);
        return R.ok(adminUserService.updateUser(dto));
    }

    @Operation(summary = "封禁账号", description = "永久封禁账号，并立即踢掉已有登录态")
    @PostMapping("/{userId}/ban")
    public R banUser(@PathVariable("userId") String userId,
                     @RequestBody(required = false) UserModerationDTO dto) {
        String reason = dto == null ? null : dto.getReason();
        return R.ok(adminUserService.banUser(userId, reason));
    }

    @Operation(summary = "解除封禁", description = "把账号恢复为正常状态")
    @PostMapping("/{userId}/unban")
    public R unbanUser(@PathVariable("userId") String userId) {
        return R.ok(adminUserService.unbanUser(userId));
    }

    @Operation(summary = "禁言账号", description = "按小时禁言账号；durationHours 为 0 或空表示永久禁言")
    @PostMapping("/{userId}/mute")
    public R muteUser(@PathVariable("userId") String userId,
                      @RequestBody(required = false) UserModerationDTO dto) {
        Integer durationHours = dto == null ? null : dto.getDurationHours();
        String reason = dto == null ? null : dto.getReason();
        return R.ok(adminUserService.muteUser(userId, durationHours, reason));
    }

    @Operation(summary = "解除禁言", description = "把账号恢复为正常状态")
    @PostMapping("/{userId}/unmute")
    public R unmuteUser(@PathVariable("userId") String userId) {
        return R.ok(adminUserService.unmuteUser(userId));
    }
}
