package com.xigua.client.controller;

import com.xigua.api.service.ContactService;
import com.xigua.common.core.base.R;
import com.xigua.domain.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @ClassName ContactController
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/7/27 9:58
 */
@Tag(name = "联系人接口")
@RequestMapping("/contact")
@RestController
public class ContactController {
    @Autowired
    private ContactService contactService;


    /**
     * 获取联系人数量
     * @author wangjinfei
     * @date 2025/7/27 9:59
     * @return R<ContactCountVO>
    */
    @ApiResponses({@ApiResponse(responseCode = "200", description = "查询成功", content =
            { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContactCountVO.class)) })})
    @Operation(summary = "获取联系人数量")
    @GetMapping("/getContactCount")
    public R<ContactCountVO> getContactCount(){
        ContactCountVO contactCount = contactService.getContactCount();
        return R.ok(contactCount);
    }

    /**
     * 获取好友列表
     * @author wangjinfei
     * @date 2025/5/14 20:56
     * @return List<FriendVO>
     */
    @ApiResponses({@ApiResponse(responseCode = "200", description = "查询成功", content =
            { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FriendVO.class)) })})
    @Operation(summary = "获取好友列表")
    @GetMapping("/getFriendList")
    public R<List<FriendVO>> getFriendList(){
        return R.ok(contactService.getFriendList());
    }

    /**
     * 获取按字母分组的好友列表
     * @author wangjinfei
     * @date 2026/4/15
     * @return List<FriendGroupVO>
     */
    @ApiResponses({@ApiResponse(responseCode = "200", description = "查询成功", content =
            { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FriendGroupVO.class)) })})
    @Operation(summary = "获取按字母分组的好友列表")
    @GetMapping("/firstLetterFriends")
    public R<List<FriendGroupVO>> firstLetterFriends(){
        return R.ok(contactService.firstLetterFriends());
    }

    /**
     * 获取群列表
     * @author wangjinfei
     * @date 2025/7/27 11:44
     * @return List<GroupVO>
     */
    @Operation(summary = "获取群列表")
    @GetMapping("/getGroupList")
    public R<List<GroupVO>> getGroupList(){
        return R.ok(contactService.getGroupList());
    }

    /**
     * 获取机器人列表
     * @author wangjinfei
     * @date 2025/10/1 16:49
     * @return List<BotVO>
     */
    @Operation(summary = "获取机器人列表")
    @GetMapping("/getBotList")
    public R<List<BotVO>> getBotList(){
        return R.ok(contactService.getBotList());
    }



    /**
     * 获取发送好友申请列表
     * @author wangjinfei
     * @date 2025/8/2 12:35
     * @return List<FriendRequestVO>
     */
    @Operation(summary = "获取发送好友申请列表")
    @GetMapping("/getSendFriendRequestList")
    public R<List<FriendRequestVO>> getSendFriendRequestList(){
        return R.ok(contactService.getSendFriendRequestList());
    }

    /**
     * 获取接收好友申请列表
     * @author wangjinfei
     * @date 2025/8/2 12:35
     * @return List<FriendRequestVO>
     */
    @Operation(summary = "获取接收好友申请列表")
    @GetMapping("/getReceiveFriendRequestList")
    public R<List<FriendRequestVO>> getReceiveFriendRequestList(){
        return R.ok(contactService.getReceiveFriendRequestList());
    }

    /**
     * 撤回我发送且仍待处理的好友申请
     * @param requestId 申请ID
     * @return Boolean
     */
    @Operation(summary = "撤回好友申请")
    @PostMapping("/withdrawFriendRequest")
    public R<Boolean> withdrawFriendRequest(@RequestParam("requestId") String requestId){
        Boolean b = contactService.withdrawFriendRequest(requestId);
        if(!b){
            return R.fail("撤回失败，申请可能已被处理");
        }
        return R.ok(true, "撤回成功");
    }

}
