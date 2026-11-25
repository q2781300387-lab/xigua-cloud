package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName UserSearchVO
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/12 21:23
 */
@Data
@Schema(title = "用户搜索vo")
public class UserSearchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Schema(description = "主键id")
    private String id;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 头像
     */
    @Schema(description = "头像")
    private String avatar;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 性别（1；男；2：女）
     */
    @Schema(description = "性别（1；男；2：女）")
    private Integer sex;

    /**
     * 是否在线
     */
    @Schema(description = "是否在线")
    private Boolean isOnline;

    /**
     * 是否好友
     */
    @Schema(description = "是否好友")
    private Boolean isFriend;
}
