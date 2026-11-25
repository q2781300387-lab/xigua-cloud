package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName ResetPasswordDTO
 * @Description 重置密码请求对象
 * @Author wangjinfei
 * @Date 2026/4/13
 */
@Data
@Schema(description = "重置密码请求对象")
public class ResetPasswordDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 邮箱地址
     */
    @NotEmpty(message = "邮箱地址不可以为空")
    @Schema(description = "邮箱地址")
    private String email;

    /**
     * 重置密码临时token
     */
    @NotEmpty(message = "重置密码token不可以为空")
    @Schema(description = "重置密码临时token")
    private String resetToken;

    /**
     * 新密码
     */
    @NotEmpty(message = "新密码不可以为空")
    @Size(min = 8, max = 20, message = "密码长度在8到20个字符之间")
    @Schema(description = "新密码")
    private String newPassword;

    /**
     * 确认密码
     */
    @NotEmpty(message = "确认密码不可以为空")
    @Schema(description = "确认密码")
    private String confirmPassword;
}
