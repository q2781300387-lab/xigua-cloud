package com.xigua.center.timed;

import com.xigua.common.core.base.R;
import com.xigua.common.core.util.UserContext;
import com.xigua.domain.dto.TimedMessageCreateDTO;
import com.xigua.domain.entity.TimedMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @ClassName TimedMessageController
 * @Description 定时消息接口（预约 / 列表 / 取消）
 */
@Tag(name = "定时消息")
@RestController
@RequestMapping("/chat/message/timed")
public class TimedMessageController {

    @Autowired
    private TimedMessageService timedMessageService;

    @Operation(summary = "预约定时消息")
    @PostMapping("/create")
    public R<TimedMessage> create(@RequestBody TimedMessageCreateDTO dto) {
        try {
            TimedMessage timedMessage = timedMessageService.create(
                    UserContext.get().getUserId(), dto.getReceiverId(), dto.getChatType(),
                    dto.getContent(), dto.getSendAt());
            return R.ok(timedMessage, "预约成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "待发送定时消息列表")
    @PostMapping("/list")
    public R<List<TimedMessage>> list(@RequestBody TimedMessageCreateDTO dto) {
        try {
            return R.ok(timedMessageService.listPending(UserContext.get().getUserId()), "查询成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "取消定时消息")
    @PostMapping("/cancel")
    public R<Boolean> cancel(@RequestBody TimedMessageCreateDTO dto) {
        try {
            return R.ok(timedMessageService.cancel(dto.getId(), UserContext.get().getUserId()), "已取消");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
