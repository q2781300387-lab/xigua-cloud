package com.xigua.domain.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @ClassName LoginDTO
 * @Description 管理端登录请求DTO
 * @Author wangjinfei
 * @Date 2026/1/22 11:09
 */
@Data
@Schema(description = "管理端登录请求参数")
public class LoginDTO {

    @Schema(description = "验证码", required = true)
    private String captcha;

    @Schema(description = "用户名", required = true)
    private String username;

    @Schema(description = "密码", required = true)
    private String password;

    @Schema(description = "验证码UUID", required = true)
    private String uuid;
}
