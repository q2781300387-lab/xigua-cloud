package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName FriendVerifyDTO
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/14 22:00
 */
@Schema(description = "好友验证DTO")
@Data
public class FriendVerifyDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "请求ID")
    private String requestId;

    @Schema(description = "好友ID")
    private String friendId;

    @Schema(description = "流程状态（0：待处理；1：同意；2：拒绝）")
    private Integer flowStatus;
}
