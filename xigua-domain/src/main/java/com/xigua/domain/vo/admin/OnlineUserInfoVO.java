package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @ClassName OnlineUserInfoVO
 * @Description 在线用户地理位置信息VO
 * @Author wangjinfei
 * @Date 2026/3/29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "在线用户地理位置信息")
public class OnlineUserInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "在线状态")
    private String onlineStatus;
}
