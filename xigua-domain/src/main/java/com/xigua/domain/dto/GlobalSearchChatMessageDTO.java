package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局搜索聊天消息请求参数（跨全部单聊、群聊、机器人会话）
 */
@Data
@Schema(title = "全局搜索聊天消息请求参数")
public class GlobalSearchChatMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索关键字", required = true)
    private String keyword;

    @Schema(description = "当前页数")
    private Integer pageNum = 1;

    @Schema(description = "每页显示个数")
    private Integer pageSize = 20;

    @Schema(description = "消息类型筛选（1文本 2图片 3语音 4视频 5文件），不传则全部")
    private Integer messageType;
}
