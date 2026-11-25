package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName AdminOnlineUserStatsVO
 * @Description 管理端在线用户统计VO
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Data
@Schema(description = "管理端在线用户统计信息")
public class AdminOnlineUserStatsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "总在线人数")
    private Long totalOnline;

    @Schema(description = "Web端在线人数")
    private Long webOnline;

    @Schema(description = "移动端在线人数")
    private Long mobileOnline;
}
