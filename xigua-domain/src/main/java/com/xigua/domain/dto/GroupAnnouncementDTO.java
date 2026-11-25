package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 群公告发布 / 编辑请求
 */
@Data
@Schema(title = "群公告请求")
public class GroupAnnouncementDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "群组id")
    private String groupId;

    @Schema(description = "公告内容")
    private String announcement;
}
