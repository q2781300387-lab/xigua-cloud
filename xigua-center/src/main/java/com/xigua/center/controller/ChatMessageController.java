package com.xigua.center.controller;

import com.xigua.center.mapper.ChatMessageMapper;
import com.xigua.common.core.util.UserContext;
import com.xigua.domain.dto.GetLastMesDTO;
import com.xigua.domain.dto.GetHistoryMes;
import com.xigua.domain.dto.GetCallRecordsDTO;
import com.xigua.domain.dto.DeleteCallRecordDTO;
import com.xigua.domain.vo.CallRecordVO;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.common.core.base.R;
import com.xigua.domain.vo.ChatMessageVO;
import com.xigua.domain.vo.LastMessageVO;
import com.xigua.api.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName ChatMessageController
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/17 19:38
 */
@Tag(name = "消息接口")
@RequestMapping("/chat/message")
@RestController
public class ChatMessageController {
    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private com.xigua.center.service.ConversationSettingService conversationSettingService;

    /**
     * 获取最后一条消息
     * @author wangjinfei
     * @date 2025/5/17 19:25
     * @param dto
     * @return List<LastChatVO>
     */
    @ApiResponses({@ApiResponse(responseCode = "200", description = "查询成功", content =
            { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LastMessageVO.class)) })})
    @Operation(summary = "获取最后一条消息")
    @PostMapping("/getLastMes")
    public R<BasePageVO<LastMessageVO>> getLastMes(@RequestBody GetLastMesDTO dto){
        BasePageVO<LastMessageVO> lastChat = chatMessageService.getLastMes(dto);
        return R.ok(lastChat, "获取成功");
    }

    /**
     * 分页获取好友历史消息
     * @author wangjinfei
     * @date 2025/5/25 10:16
     * @param dto
     * @return BasePageVO<ChatMessageVO>
     */
    @Operation(summary = "分页获取好友历史消息")
    @PostMapping("/getHistoryMes")
    public R<BasePageVO<ChatMessageVO>> getHistoryMes(@RequestBody GetHistoryMes dto){
        return R.ok(chatMessageService.getHistoryMes(dto), "查询成功");
    }

    @Operation(summary = "分页查询视频通话记录")
    @PostMapping("/callRecords")
    public R<BasePageVO<CallRecordVO>> callRecords(@RequestBody GetCallRecordsDTO dto) {
        return R.ok(chatMessageService.getCallRecords(dto), "查询成功");
    }

    @Operation(summary = "未接来电角标数量")
    @PostMapping("/callRecords/unreadCount")
    public R<Integer> callRecordsUnreadCount() {
        return R.ok(chatMessageService.countUnreadCallRecords(), "查询成功");
    }

    @Operation(summary = "清空通话记录未接角标")
    @PostMapping("/callRecords/read")
    public R<Void> callRecordsRead() {
        chatMessageService.markCallRecordsRead();
        return R.ok(null, "已读");
    }

    @Operation(summary = "删除单条通话记录")
    @PostMapping("/callRecords/delete")
    public R<Void> deleteCallRecord(@RequestBody DeleteCallRecordDTO dto) {
        chatMessageService.deleteCallRecord(dto.getMessageId());
        return R.ok(null, "已删除");
    }

    @Operation(summary = "清空全部通话记录")
    @PostMapping("/callRecords/clear")
    public R<Void> clearCallRecords() {
        chatMessageService.clearCallRecords();
        return R.ok(null, "已清空");
    }

    /**
     * 查询当前用户未读且被 @ 的会话（离线期间补拉「@我」提醒）
     * @param body userId
     * @return 会话 id 列表
     */
    @Operation(summary = "查询未读且@我的会话")
    @PostMapping("/atMeUnread")
    public R<java.util.List<String>> atMeUnread(@RequestBody java.util.Map<String, String> body){
        // 只能查当前登录人自己的 @ 提醒，请求体里的 userId 仅兼容旧前端，不参与查询
        String userId = UserContext.get().getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return R.fail("用户未登录");
        }
        return R.ok(chatMessageMapper.selectAtMeChatIds(userId.trim()), "查询成功");
    }

    @Operation(summary = "设置会话置顶/草稿")
    @PostMapping("/conversation/setting")
    public R<com.xigua.domain.vo.ConversationSettingVO> saveConversationSetting(
            @RequestBody com.xigua.domain.dto.ConversationSettingDTO dto){
        String userId = UserContext.get().getUserId();
        return R.ok(conversationSettingService.save(userId, dto), "已保存");
    }

    @Operation(summary = "查询会话置顶/草稿")
    @PostMapping("/conversation/setting/get")
    public R<com.xigua.domain.vo.ConversationSettingVO> getConversationSetting(
            @RequestBody com.xigua.domain.dto.ConversationSettingDTO dto){
        String userId = UserContext.get().getUserId();
        return R.ok(conversationSettingService.get(userId, dto.getChatId(), dto.getChatType()), "查询成功");
    }

    @Operation(summary = "编辑文本消息")
    @PostMapping("/edit")
    public R<Void> editMessage(@RequestBody com.xigua.domain.dto.EditChatMessageDTO dto){
        chatMessageService.editMessage(dto.getMessageId(), dto.getContent());
        return R.ok(null, "已保存");
    }
}
