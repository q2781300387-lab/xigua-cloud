package com.xigua.domain.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端用户封禁/禁言参数。
 */
@Data
@Schema(description = "管理端用户封禁/禁言参数")
public class UserModerationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "操作原因，最多 200 字")
    private String reason;

    @Schema(description = "禁言时长（小时）；0 或为空表示永久")
    private Integer durationHours;
}
