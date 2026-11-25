package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName receiveFriendRequestVO
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/13 23:09
 */
@Schema(description = "好友申请VO")
@Data
public class FriendRequestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "请求ID")
    private String requestId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "申请消息")
    private String applyMsg;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "流程状态（0：待处理；1：同意；2：拒绝）")
    private Integer flowStatus;
}
