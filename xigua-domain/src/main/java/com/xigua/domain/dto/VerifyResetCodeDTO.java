package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName VerifyResetCodeDTO
 * @Description 验证重置密码验证码请求对象
 * @Author wangjinfei
 * @Date 2026/4/13
 */
@Data
@Schema(description = "验证重置密码验证码请求对象")
public class VerifyResetCodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 邮箱地址
     */
    @NotEmpty(message = "邮箱地址不可以为空")
    @Schema(description = "邮箱地址")
    private String email;

    /**
     * 邮箱验证码
     */
    @NotEmpty(message = "验证码不可以为空")
    @Schema(description = "邮箱验证码")
    private String code;
}
