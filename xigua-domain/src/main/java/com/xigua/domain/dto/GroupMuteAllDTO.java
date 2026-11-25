package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 全员禁言开关请求
 */
@Data
@Schema(title = "全员禁言请求")
public class GroupMuteAllDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "群组id")
    private String groupId;

    @Schema(description = "全员禁言（0：关闭；1：开启）")
    private Integer muteAll;
}
