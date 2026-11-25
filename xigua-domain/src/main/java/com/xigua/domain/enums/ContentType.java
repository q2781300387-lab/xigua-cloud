package com.xigua.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public enum ContentType {
    // 消息类型（1文本 2图片 3语音 4视频 5文件）
    TEXT(1, "文本"),
    IMAGE(2, "图片"),
    AUDIO(3, "语音"),
    VIDEO(4, "视频"),
    FILE(5, "文件"),
    CALL(9, "视频通话"),
    CHAT_RECORD(7, "聊天记录"),
    VOTE(8, "群投票"),
    ;

    final Integer value;

    final String desc;
}
