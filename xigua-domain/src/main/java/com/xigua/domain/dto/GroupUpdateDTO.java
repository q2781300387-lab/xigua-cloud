package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 群资料编辑请求（群名称 / 群头像 / 群简介）
 */
@Data
@Schema(title = "群资料编辑请求")
public class GroupUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "群组id")
    private String groupId;

    @Schema(description = "群名称")
    private String groupName;

    @Schema(description = "群头像地址")
    private String groupAvatar;

    @Schema(description = "群简介")
    private String description;
}
