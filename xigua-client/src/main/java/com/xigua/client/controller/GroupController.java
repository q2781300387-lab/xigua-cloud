package com.xigua.client.controller;

import com.xigua.common.core.exception.BusinessException;
import com.xigua.domain.dto.GroupDTO;
import com.xigua.domain.dto.GroupInviteDTO;
import com.xigua.common.core.base.R;
import com.xigua.api.service.GroupService;
import com.xigua.domain.vo.GroupDetailVO;
import com.xigua.domain.vo.UserSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName GroupController
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/7/6 15:30
 */
@Tag(name = "群组接口")
@Slf4j
@RequestMapping("/group")
@RestController
public class GroupController {
    @Autowired
    private GroupService groupService;

    /**
     * 创建群组
     * @author wangjinfei
     * @date 2025/7/6 15:32
     * @param dto
     * @return R<GetLocationRes>
    */
    @Operation(summary = "创建群组")
    @PostMapping("/createGroup")
    public R<String> createGroup(@RequestBody GroupDTO dto){
        Boolean b = null;
        try {
            b = groupService.createGroup(dto);
        } catch (BusinessException e){
            log.error("创建群组失败：{}", e.getMessage(), e);
            throw new BusinessException(e.getMessage());
        }catch (Exception e) {
            log.error("创建群组失败：{}", e.getMessage(), e);
            throw new BusinessException("创建群组失败");
        }
        if (!b){
            return R.fail("创建失败");
        }
        return R.ok("创建成功");
    }

    /**
     * 获取群组详情
     * @author wangjinfei
     * @date 2025/7/30 19:35
     * @param groupId
     * @return GroupDetailVO
     */
    @ApiResponses({@ApiResponse(responseCode = "200", description = "查询成功", content =
            { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = GroupDetailVO.class)) })})
    @Operation(summary = "获取群组详情")
    @GetMapping("/getGroupDetail")
    public R<GroupDetailVO> getGroupDetail(@RequestParam String groupId){
        return R.ok(groupService.getGroupDetail(groupId));
    }

    /**
     * 获取群成员用户信息列表（@提及选人用）
     * @author doubao
     * @date 2026/11/4
     * @param groupId 群组id
     * @return List<UserSearchVO>
     */
    @Operation(summary = "获取群成员用户信息列表")
    @GetMapping("/getGroupMembers")
    public R<List<UserSearchVO>> getGroupMembers(@RequestParam String groupId){
        return R.ok(groupService.getGroupMemberUsers(groupId));
    }

    /**
     * 退出群聊
     * 群主不可退出，需走解散
     * @author doubao
     * @date 2026/11/6
     * @param groupId 群组id
     * @return Boolean
     */
    @Operation(summary = "退出群聊")
    @PostMapping("/quitGroup")
    public R<Boolean> quitGroup(@RequestParam String groupId){
        return R.ok(groupService.quitGroup(groupId));
    }

    /**
     * 解散群聊
     * 仅群主可操作
     * @author doubao
     * @date 2026/11/6
     * @param groupId 群组id
     * @return Boolean
     */
    @Operation(summary = "解散群聊")
    @PostMapping("/disbandGroup")
    public R<Boolean> disbandGroup(@RequestParam String groupId){
        return R.ok(groupService.disbandGroup(groupId));
    }

    /**
     * 邀请成员入群
     * 群主或管理员可操作，成功后群内收到系统消息
     * @author doubao
     * @date 2026/11/12
     * @param dto 群id + 被邀请用户id列表
     * @return Integer 实际入群人数
     */
    @Operation(summary = "邀请成员入群")
    @PostMapping("/inviteMembers")
    public R<Integer> inviteMembers(@RequestBody GroupInviteDTO dto){
        return R.ok(groupService.inviteMembers(dto.getGroupId(), dto.getUserIds()));
    }

    /**
     * 将成员移出群聊
     * 群主可移出任何人，管理员只能移出普通成员
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param targetUserId 被移出用户id
     * @return Boolean
     */
    @Operation(summary = "将成员移出群聊")
    @PostMapping("/kickMember")
    public R<Boolean> kickMember(@RequestParam String groupId, @RequestParam String targetUserId){
        return R.ok(groupService.kickMember(groupId, targetUserId));
    }

    /**
     * 设置成员角色（仅群主；2=管理员，3=普通成员）
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param targetUserId 目标用户id
     * @param role 目标角色
     * @return Boolean
     */
    @Operation(summary = "设置成员角色")
    @PostMapping("/setMemberRole")
    public R<Boolean> setMemberRole(@RequestParam String groupId, @RequestParam String targetUserId, @RequestParam Integer role){
        return R.ok(groupService.setMemberRole(groupId, targetUserId, role));
    }

    /**
     * 修改群资料（群名称 / 群头像 / 群简介；群主或管理员）
     */
    @Operation(summary = "修改群资料")
    @PostMapping("/updateGroupInfo")
    public R<Boolean> updateGroupInfo(@RequestBody com.xigua.domain.dto.GroupUpdateDTO dto){
        return R.ok(groupService.updateGroupInfo(dto));
    }

    /**
     * 发布 / 编辑 / 清空群公告（群主或管理员）
     */
    @Operation(summary = "发布或编辑群公告")
    @PostMapping("/updateAnnouncement")
    public R<Boolean> updateAnnouncement(@RequestBody com.xigua.domain.dto.GroupAnnouncementDTO dto){
        return R.ok(groupService.updateAnnouncement(dto.getGroupId(), dto.getAnnouncement()));
    }

    /**
     * 开启 / 关闭全员禁言（群主或管理员）
     */
    @Operation(summary = "开启或关闭全员禁言")
    @PostMapping("/updateMuteAll")
    public R<Boolean> updateMuteAll(@RequestBody com.xigua.domain.dto.GroupMuteAllDTO dto){
        return R.ok(groupService.updateMuteAll(dto.getGroupId(), dto.getMuteAll()));
    }

    /**
     * 单人禁言 / 解除禁言
     * durationMinutes 为 0 表示解除禁言
     */
    @Operation(summary = "禁言或解除禁言群成员")
    @PostMapping("/muteMember")
    public R<Boolean> muteMember(@RequestBody com.xigua.domain.dto.GroupMuteMemberDTO dto){
        return R.ok(groupService.muteMember(dto.getGroupId(), dto.getTargetUserId(), dto.getDurationMinutes()));
    }

    /**
     * 转让群主（仅群主）
     */
    @Operation(summary = "转让群主")
    @PostMapping("/transferOwner")
    public R<Boolean> transferOwner(@RequestParam String groupId, @RequestParam String targetUserId){
        return R.ok(groupService.transferOwner(groupId, targetUserId));
    }

    @Operation(summary = "按群号或群名称搜索群聊")
    @GetMapping("/search")
    public R<List<com.xigua.domain.vo.GroupJoinRequestVO>> search(@RequestParam String keyword){
        return R.ok(groupService.searchGroups(keyword));
    }

    @Operation(summary = "提交入群申请")
    @PostMapping("/joinRequest")
    public R<Boolean> joinRequest(@RequestBody com.xigua.domain.dto.GroupJoinRequestDTO dto){
        return R.ok(groupService.requestJoin(dto.getGroupId(), dto.getReason()), "已提交申请，等待群主审批");
    }

    @Operation(summary = "查询待审批入群申请")
    @GetMapping("/joinRequests")
    public R<List<com.xigua.domain.vo.GroupJoinRequestVO>> joinRequests(@RequestParam String groupId){
        return R.ok(groupService.listJoinRequests(groupId));
    }

    @Operation(summary = "审批入群申请")
    @PostMapping("/handleJoinRequest")
    public R<Boolean> handleJoinRequest(@RequestBody com.xigua.domain.dto.GroupJoinHandleDTO dto){
        return R.ok(groupService.handleJoinRequest(dto.getRequestId(), dto.getApprove()), "已处理");
    }

    @Operation(summary = "发起群通话（4 人以内 mesh）")
    @PostMapping("/call/start")
    public R<com.xigua.domain.vo.GroupCallRoomVO> startCall(@RequestParam String groupId,
                                                            @RequestParam(defaultValue = "true") Boolean video){
        return R.ok(groupService.startGroupCall(groupId, video));
    }

    @Operation(summary = "加入群通话")
    @PostMapping("/call/join")
    public R<com.xigua.domain.vo.GroupCallRoomVO> joinCall(@RequestParam String groupId,
                                                           @RequestParam String callId){
        return R.ok(groupService.joinGroupCall(groupId, callId));
    }

    @Operation(summary = "查询群通话状态")
    @GetMapping("/call/state")
    public R<com.xigua.domain.vo.GroupCallRoomVO> callState(@RequestParam String groupId){
        return R.ok(groupService.getGroupCallState(groupId));
    }

    @Operation(summary = "离开群通话")
    @PostMapping("/call/leave")
    public R<Boolean> leaveCall(@RequestParam String groupId, @RequestParam String callId){
        return R.ok(groupService.leaveGroupCall(groupId, callId));
    }

    @Operation(summary = "发送群通话 WebRTC 信令")
    @PostMapping("/call/signal")
    public R<Boolean> callSignal(@RequestBody com.xigua.domain.dto.GroupCallSignalDTO dto){
        return R.ok(groupService.sendGroupCallSignal(dto));
    }

    @Operation(summary = "拉取群通话信令")
    @GetMapping("/call/signals")
    public R<List<com.xigua.domain.vo.GroupCallSignalVO>> callSignals(@RequestParam String groupId,
                                                                      @RequestParam String callId){
        return R.ok(groupService.pollGroupCallSignals(groupId, callId));
    }
}
