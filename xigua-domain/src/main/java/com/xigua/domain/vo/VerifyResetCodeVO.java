package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @ClassName VerifyResetCodeVO
 * @Description 验证重置密码验证码响应对象
 * @Author wangjinfei
 * @Date 2026/4/13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证重置密码验证码响应对象")
public class VerifyResetCodeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 重置密码临时token（有效期10分钟）
     */
    @Schema(description = "重置密码临时token（有效期10分钟）")
    private String resetToken;
}
