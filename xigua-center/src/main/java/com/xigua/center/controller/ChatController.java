package com.xigua.center.controller;

import com.xigua.api.service.ChatService;
import com.xigua.domain.dto.OpenChatDTO;
import com.xigua.common.core.base.R;
import com.xigua.domain.vo.OpenChatVO;
import com.xigua.domain.vo.UnreadInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName ChatController
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/12/6 14:55
 */
@Tag(name = "聊天接口")
@RequestMapping("/chat")
@RestController
public class ChatController {
    @Autowired
    private ChatService chatService;

    @ApiResponses({@ApiResponse(responseCode = "200", description = "查询成功", content =
            { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = OpenChatVO.class)) })})
    @Operation(summary = "打开聊天")
    @PostMapping("/openChat")
    public R<OpenChatVO> openChat(@RequestBody OpenChatDTO dto){
        return R.ok(chatService.openChat(dto.getChatId(), dto.getChatType()));
    }

    @Operation(summary = "关闭聊天")
    @PostMapping("/closeChat")
    public R<Boolean> closeChat(@RequestParam("chatId") String chatId){
        Boolean closeChat = chatService.closeChat(chatId);
        if(closeChat){
            return R.ok(true,"关闭成功");
        }
        return R.ok(false,"关闭失败");
    }

    @Operation(summary = "隐藏聊天")
    @PostMapping("/hideChat")
    public R<Boolean> hideChat(@RequestParam("chatId") String chatId){
        Boolean hideChat = chatService.hideChat(chatId);
        if(hideChat){
            return R.ok(true,"隐藏成功");
        }
        return R.ok(false,"隐藏失败");
    }

    @ApiResponses({@ApiResponse(responseCode = "200", description = "查询成功", content =
            { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UnreadInfoVO.class)) })})
    @Operation(summary = "获取未读信息")
    @GetMapping("/getUnreadInfo")
    public R<UnreadInfoVO> getUnreadInfo(){
        UnreadInfoVO unreadInfo = chatService.getUnreadInfo();
        return R.ok(unreadInfo);
    }
}
