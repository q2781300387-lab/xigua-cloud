package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName GroupMemberLastMessageVO
 * @Description 群成员最后发言时间VO
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Data
@Schema(description = "群成员最后发言时间")
public class GroupMemberLastMessageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "发送人ID")
    private String senderId;

    @Schema(description = "最后发言时间")
    private LocalDateTime lastMessageTime;
}
