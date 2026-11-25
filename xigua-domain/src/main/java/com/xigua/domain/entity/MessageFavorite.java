package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息收藏：保存消息快照，原消息撤回/删除后收藏仍可查看。
 */
@Data
@TableName("xg_message_favorite")
public class MessageFavorite implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    private String userId;

    private String messageId;

    private Integer chatType;

    private String peerId;

    private String chatName;

    private String senderId;

    private String senderName;

    private Integer messageType;

    private String content;

    private LocalDateTime originalTime;

    private LocalDateTime createTime;
}
