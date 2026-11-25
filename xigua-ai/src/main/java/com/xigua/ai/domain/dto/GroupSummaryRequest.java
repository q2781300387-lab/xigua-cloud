package com.xigua.ai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName GroupSummaryRequest
 * @Description 群聊/会话智能摘要请求：传入最近消息列表，由 LLM 生成摘要
 * @Author xigua
 * @Date 2026/10/16 19:30
 */
@Data
@Schema(title = "智能摘要请求")
public class GroupSummaryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话类型（1单聊 2群聊），仅用于提示词场景
     */
    @Schema(description = "会话类型 1单聊 2群聊")
    private Integer chatType;

    /**
     * 最近消息列表
     */
    @Schema(description = "最近消息列表")
    private List<SummaryMessage> messages;

    @Data
    public static class SummaryMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        /**
         * 发送者昵称
         */
        @Schema(description = "发送者昵称")
        private String name;
        /**
         * 消息内容
         */
        @Schema(description = "消息内容")
        private String content;
    }
}
