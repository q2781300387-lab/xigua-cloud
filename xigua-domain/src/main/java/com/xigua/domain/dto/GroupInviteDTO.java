package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 邀请成员入群请求
 */
@Data
@Schema(title = "邀请成员入群请求")
public class GroupInviteDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "群组id")
    private String groupId;

    @Schema(description = "被邀请的用户id列表")
    private List<String> userIds;
}
