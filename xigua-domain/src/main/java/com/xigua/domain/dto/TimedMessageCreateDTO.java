package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName TimedMessageCreateDTO
 * @Description 预约定时消息请求
 */
@Data
@Schema(description = "预约定时消息")
public class TimedMessageCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "发送人id")
    private String senderId;

    @Schema(description = "定时消息id（取消时使用）")
    private String id;

    @Schema(description = "接收人id / 群id")
    private String receiverId;

    @Schema(description = "聊天类型 1单聊 2群聊 3机器人")
    private Integer chatType;

    @Schema(description = "消息正文")
    private String content;

    @Schema(description = "预定发送时间 yyyy-MM-dd HH:mm:ss")
    private String sendAt;
}
