package com.xigua.domain.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName AdminGroupMemberPageVO
 * @Description 管理端群成员分页VO
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Data
@Schema(description = "管理端群成员分页信息")
public class AdminGroupMemberPageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "群内昵称")
    private String nickname;

    @Schema(description = "群内角色（1：群主；2：群管理；3：普通成员）")
    private Integer role;

    @Schema(description = "群内角色名称")
    private String roleName;

    @Schema(description = "入群时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinTime;

    @Schema(description = "最后发言时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageTime;

    @Schema(description = "头像")
    private String avatar;
}
