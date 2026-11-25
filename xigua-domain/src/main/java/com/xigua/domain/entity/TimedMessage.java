package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName TimedMessage
 * @Description 定时消息（服务端持久化，到点由调度任务投递，页面关闭不影响发送）
 */
@Data
@TableName("xg_timed_message")
@Schema(description = "定时消息")
public class TimedMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键id")
    @TableId
    private String id;

    @Schema(description = "发送人id")
    private String senderId;

    @Schema(description = "接收人id / 群id")
    private String receiverId;

    @Schema(description = "聊天类型 1单聊 2群聊 3机器人")
    private Integer chatType;

    @Schema(description = "消息正文")
    private String content;

    @Schema(description = "预定发送时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendAt;

    @Schema(description = "状态 0待发 1已发 2失败 3已取消")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
