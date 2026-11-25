package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 群成员视图（群详情成员列表用，带角色与入群时间）
 */
@Data
@Schema(title = "群成员视图")
public class GroupMemberVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "群内角色（1：群主；2：群管理；3：普通成员）")
    private Integer role;

    @Schema(description = "角色文案")
    private String roleName;

    @Schema(description = "是否群主")
    private Boolean owner;

    @Schema(description = "入群时间")
    private String joinTime;

    @Schema(description = "单人禁言截止时间，空表示未禁言")
    private String muteUntil;

    @Schema(description = "当前是否处于禁言中")
    private Boolean muted;
}
