package com.xigua.domain.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端群全员禁言参数。
 */
@Data
@Schema(description = "管理端群全员禁言参数")
public class GroupMuteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "全员禁言（0：关闭；1：开启）")
    private Integer muteAll;
}
