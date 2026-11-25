package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName UnreadInfoVO
 * @Description 未读信息
 * @Author wangjinfei
 * @Date 2026/4/21 20:30
 */
@Data
@Schema(description = "未读信息")
public class UnreadInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "总未读数量（单聊+群聊）")
    private Long chatUnreadCount;

    // todo 后续可扩展的字段
    // @Schema(description = "是否有未读通知")
    // private Boolean hasUnreadNotification;
    // @Schema(description = "系统通知未读数量")
    // private Long systemNotificationCount;
}
