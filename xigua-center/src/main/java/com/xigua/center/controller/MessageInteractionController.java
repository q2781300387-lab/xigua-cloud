package com.xigua.center.controller;

import com.xigua.center.service.MessageInteractionService;
import com.xigua.common.core.base.R;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.dto.MessageFavoriteDTO;
import com.xigua.domain.dto.MessageReactionDTO;
import com.xigua.domain.dto.MessageReactionQueryDTO;
import com.xigua.domain.vo.MessageFavoriteVO;
import com.xigua.domain.vo.MessageInteractionVO;
import com.xigua.domain.vo.MessageReactionSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息互动：表情回应、收藏夹。
 */
@Tag(name = "消息互动")
@RestController
@RequestMapping("/chat/message")
public class MessageInteractionController {

    @Autowired
    private MessageInteractionService messageInteractionService;

    @Operation(summary = "批量查询消息回应与收藏状态")
    @PostMapping("/interactions")
    public R<MessageInteractionVO> interactions(@RequestBody MessageReactionQueryDTO dto) {
        return R.ok(messageInteractionService.interactions(dto), "查询成功");
    }

    @Operation(summary = "切换消息表情回应")
    @PostMapping("/reaction/toggle")
    public R<MessageReactionSummaryVO> toggleReaction(@RequestBody MessageReactionDTO dto) {
        return R.ok(messageInteractionService.toggleReaction(dto), "操作成功");
    }

    @Operation(summary = "收藏 / 取消收藏消息")
    @PostMapping("/favorite/toggle")
    public R<Boolean> toggleFavorite(@RequestBody MessageFavoriteDTO dto) {
        return R.ok(messageInteractionService.toggleFavorite(dto), "操作成功");
    }

    @Operation(summary = "收藏夹分页列表")
    @PostMapping("/favorites")
    public R<BasePageVO<MessageFavoriteVO>> favorites(@RequestBody MessageFavoriteDTO dto) {
        return R.ok(messageInteractionService.favorites(dto), "查询成功");
    }

    @Operation(summary = "移除收藏")
    @PostMapping("/favorite/remove")
    public R<Boolean> removeFavorite(@RequestBody MessageFavoriteDTO dto) {
        return R.ok(messageInteractionService.removeFavorite(dto == null ? null : dto.getMessageId()), "已移除");
    }
}
