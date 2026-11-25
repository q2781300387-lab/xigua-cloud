package com.xigua.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName LoginLog
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/15 9:50
 */
@Data
public class LoginLogSuccessBO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private String userId;

    /**
     * 登录方式（1：密码）
     */
    @Schema(description = "登录方式（1：密码）")
    private Integer loginType;

    /**
     * 登录ip
     */
    @Schema(description = "登录ip")
    private String loginIp;

    /**
     * ua
     */
    @Schema(description = "ua")
    private String userAgent;

    /**
     * 平台（1：web；2：app；3：mini）
     */
    @Schema(description = "平台（1：web；2：app；3：mini）")
    private Integer platform;
}
