package com.xigua.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessageFavoriteDTO implements Serializable {
    private String messageId;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String keyword;
    /** null=全部，1文本 2图片 3语音 4视频 5文件 */
    private Integer messageType;
}
