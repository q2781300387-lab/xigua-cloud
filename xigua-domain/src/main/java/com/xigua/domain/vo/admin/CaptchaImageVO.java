package com.xigua.domain.vo.admin;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName CaptchaImageVO
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/21 21:26
 */
@Data
public class CaptchaImageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 过期时间（时间戳，毫秒）
     */
    private long expireTime;

    /**
     * base64 图片
     */
    private String img;

    /**
     * 是否启用验证码
     */
    private boolean isEnabled;

    /**
     * 唯一标识，用于校验
     */
    private String uuid;
}
