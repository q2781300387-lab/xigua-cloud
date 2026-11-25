package com.xigua.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MessageFavoriteVO implements Serializable {
    private String id;
    private String messageId;
    private Integer chatType;
    private String peerId;
    private String chatName;
    private String senderId;
    private String senderName;
    private Integer messageType;
    private String content;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime originalTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
