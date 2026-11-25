package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息表情回应：一条消息下每个用户最多保留一个回应，切换表情时覆盖。
 */
@Data
@TableName("xg_message_reaction")
public class MessageReaction implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    private String messageId;

    private String userId;

    private String emoji;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
