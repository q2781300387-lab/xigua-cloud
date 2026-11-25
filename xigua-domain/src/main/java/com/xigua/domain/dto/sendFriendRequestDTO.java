package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName sendFriendRequestDTO
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/13 21:30
 */
@Schema(description = "发送好友请求DTO")
@Data
public class SendFriendRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "好友ID")
    private String friendId;

    @Schema(description = "申请消息")
    private String applyMsg;
}
