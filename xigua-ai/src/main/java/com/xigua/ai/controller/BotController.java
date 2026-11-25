package com.xigua.ai.controller;

import com.xigua.api.service.BotService;
import com.xigua.common.core.util.UserContext;
import com.xigua.domain.dto.BotDTO;
import com.xigua.common.core.base.R;
import com.xigua.domain.entity.Bot;
import com.xigua.domain.vo.BotDetailVO;
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
 * @ClassName BotController
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/10/1 15:35
 */
@Tag(name = "机器人接口")
@Slf4j
@RequestMapping("/bot")
@RestController
public class BotController {
    @Autowired
    private BotService botService;

    @Operation(summary = "创建机器人")
    @PostMapping("/createBot")
    public R<Boolean> createBot(@RequestBody BotDTO dto) {
        Boolean b = botService.create(dto);
        if (b) {
            return R.ok(b, "创建成功");
        }
        return R.fail("创建失败");
    }

    @Operation(summary = "更新机器人")
    @PostMapping("/updateBot")
    public R<Boolean> updateBot(@RequestParam("botId") String botId, @RequestBody BotDTO dto) {
        Boolean b = botService.update(botId, dto, UserContext.get().getUserId());
        if (b) {
            return R.ok(b, "更新成功");
        }
        return R.fail("更新失败");
    }

    /**
     * 获取机器人详情
     * @author wangjinfei
     * @date 2025/10/8 18:19
     * @param botId
     * @return BotDetailVO
     */
    @ApiResponses({@ApiResponse(responseCode = "200", description = "查询成功", content =
            { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BotDetailVO.class)) })})
    @Operation(summary = "获取机器人详情")
    @PostMapping("/getBotDetail")
    public R<BotDetailVO> getBotDetail(@RequestParam("botId") String botId){
        BotDetailVO botDetail = botService.getBotDetail(botId);
        return R.ok(botDetail);
    }

    @Operation(summary = "获取机器人列表")
    @PostMapping("/getBots")
    public R<List<Bot>> getBots() {
        return R.ok(botService.getBots(UserContext.get().getUserId()));
    }

    @Operation(summary = "删除机器人")
    @PostMapping("/deleteBot")
    public R<Boolean> deleteBot(@RequestParam("botId") String botId) {
        Boolean b = botService.delete(botId, UserContext.get().getUserId());
        if (b) {
            return R.ok(b, "删除成功");
        }
        return R.fail("删除失败");
    }
}
