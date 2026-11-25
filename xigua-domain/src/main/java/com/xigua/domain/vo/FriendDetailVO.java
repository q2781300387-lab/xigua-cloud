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
public class FriendDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private String userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "用户头像")
    private String avatar;
    @Schema(description = "用户签名")
    private String signature;
    @Schema(description = "用户性别（1；男；2：女）")
    private Integer sex;
    @Schema(description = "用户区域")
    private String region;
    @Schema(description = "用户邮箱")
    private String email;
    @Schema(description = "用户手机号")
    private String phone;
    @Schema(description = "用户注册时间")
    private String registerTime;
    @Schema(description = "创建好友时间")
    private String createFriendTime;
    @Schema(description = "是否在线")
    private Boolean isOnline;
}
