package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName SearchChatMessageDTO
 * @Description 搜索聊天记录请求参数（仅依赖 ES，不携带 mybatis-plus 分页基类）
 * @Author wangjinfei
 * @Date 2026/9/29
 */
@Data
@Schema(title = "搜索聊天记录请求参数")
public class SearchChatMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索关键字", required = true)
    private String keyword;

    @Schema(description = "聊天类型（1：单聊；2：群聊；3：机器人）", required = true)
    private Integer chatType;

    @Schema(description = "对方id（好友id/群id/机器人id）", required = true)
    private String receiverId;

    @Schema(description = "当前页数")
    private Integer pageNum = 1;

    @Schema(description = "每页显示个数")
    private Integer pageSize = 20;
}
