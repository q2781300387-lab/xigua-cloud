package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName UpdateUserDTO
 * @Description 管理端更新用户DTO
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Data
@Schema(description = "管理端更新用户信息")
public class UpdateUserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", required = true)
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "性别（1：男；2：女）")
    private Integer sex;

    @Schema(description = "地区")
    private String region;

    @Schema(description = "个性签名")
    private String signature;
}
