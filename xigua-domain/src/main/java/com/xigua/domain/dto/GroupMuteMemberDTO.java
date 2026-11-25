package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 群成员单人禁言请求
 */
@Data
@Schema(title = "群成员单人禁言请求")
public class GroupMuteMemberDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "群组id")
    private String groupId;

    @Schema(description = "目标成员id")
    private String targetUserId;

    @Schema(description = "禁言分钟数，0 表示解除禁言")
    private Integer durationMinutes;
}
