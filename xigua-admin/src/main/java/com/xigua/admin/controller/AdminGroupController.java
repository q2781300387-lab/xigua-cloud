package com.xigua.admin.controller;

import com.xigua.api.service.admin.AdminGroupService;
import com.xigua.common.core.base.R;
import com.xigua.domain.dto.QueryGroupMemberPageDTO;
import com.xigua.domain.dto.QueryGroupPageDTO;
import com.xigua.domain.dto.admin.GroupMuteDTO;
import com.xigua.domain.vo.admin.AdminGroupDetailVO;
import com.xigua.domain.vo.admin.AdminGroupMemberPageVO;
import com.xigua.domain.vo.admin.AdminGroupPageVO;
import com.xigua.domain.vo.admin.AdminGroupStatisticsVO;
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
 * @ClassName AdminGroupController
 * @Description 管理端群组接口
 * @Author wangjinfei
 * @Date 2026/3/7
 */
@Tag(name = "管理端群组接口", description = "提供管理端群组管理相关功能")
@RequestMapping("/group")
@RestController
public class AdminGroupController {

    @Autowired
    private AdminGroupService adminGroupService;

    @Operation(summary = "分页查询群组", description = "根据群组名称、群主名称、创建时间等条件分页查询群组列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AdminGroupPageVO.class)))
    })
    @PostMapping("/page")
    public R page(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "分页查询参数", required = true) @RequestBody QueryGroupPageDTO dto) {
        return R.ok(adminGroupService.page(dto));
    }

    @Operation(summary = "查询群组详情", description = "根据群组ID查询群组详细信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AdminGroupDetailVO.class)))
    })
    @GetMapping("/{groupId}")
    public R getDetail(@Parameter(description = "群组ID", required = true) @PathVariable("groupId") String groupId) {
        return R.ok(adminGroupService.getDetail(groupId));
    }

    @Operation(summary = "获取群组统计信息", description = "获取成员总数、昨日活跃人数、累计消息量及增长率")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AdminGroupStatisticsVO.class)))
    })
    @GetMapping("/statistics/{groupId}")
    public R getStatistics(@Parameter(description = "群组ID", required = true) @PathVariable("groupId") String groupId) {
        return R.ok(adminGroupService.getStatistics(groupId));
    }

    @Operation(summary = "分页查询群成员", description = "根据成员名称或群内昵称分页查询群成员列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AdminGroupMemberPageVO.class)))
    })
    @PostMapping("/{groupId}/members/page")
    public R pageMembers(@Parameter(description = "群组ID", required = true) @PathVariable("groupId") String groupId,
                         @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "分页查询参数", required = true)
                         @RequestBody QueryGroupMemberPageDTO dto) {
        return R.ok(adminGroupService.pageMembers(groupId, dto));
    }

    @Operation(summary = "设置全员禁言", description = "管理端直接开启/关闭群全员禁言，不依赖群主身份")
    @PutMapping("/{groupId}/mute-all")
    public R setMuteAll(@PathVariable("groupId") String groupId,
                        @RequestBody GroupMuteDTO dto) {
        return R.ok(adminGroupService.setMuteAll(groupId, dto == null ? null : dto.getMuteAll()));
    }

    @Operation(summary = "解散群聊", description = "管理端直接解散群聊，清理成员关系与会话缓存")
    @PostMapping("/{groupId}/disband")
    public R disbandGroup(@PathVariable("groupId") String groupId) {
        return R.ok(adminGroupService.disbandGroup(groupId));
    }

    @Operation(summary = "移出群成员", description = "管理端把指定成员移出群聊，群主不可移出")
    @PostMapping("/{groupId}/members/{userId}/remove")
    public R removeMember(@PathVariable("groupId") String groupId,
                          @PathVariable("userId") String userId) {
        return R.ok(adminGroupService.removeMember(groupId, userId));
    }
}
