package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName FriendVO
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/13 23:09
 */
@Data
public class FriendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "好友ID")
    private String friendId;
    @Schema(description = "用户ID")
    private String userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "用户头像")
    private String avatar;
    @Schema(description = "用户签名")
    private String signature;
    @Schema(description = "是否在线")
    private Boolean isOnline;
}
