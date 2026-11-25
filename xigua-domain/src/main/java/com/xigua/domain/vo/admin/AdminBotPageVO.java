package com.xigua.domain.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName AdminBotPageVO
 * @Description 管理端机器人分页VO
 * @Author wangjinfei
 * @Date 2026/3/29
 */
@Data
@Schema(description = "管理端机器人分页信息")
public class AdminBotPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "机器人ID")
    private String id;

    @Schema(description = "机器人名称")
    private String name;

    @Schema(description = "机器人头像")
    private String avatar;

    @Schema(description = "所属用户ID")
    private String userId;

    @Schema(description = "所属用户名")
    private String username;

    @Schema(description = "所属用户头像")
    private String userAvatar;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
