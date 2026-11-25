package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName ContactCountVO
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/7/27 9:40
 */
@Data
public class ContactCountVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "好友数量")
    private Integer friendCount;

    @Schema(description = "群数量")
    private Integer groupCount;

    @Schema(description = "机器人数量")
    private Integer botCount;

    @Schema(description = "发送消息数量")
    private Integer sendCount;

    @Schema(description = "接收消息数量")
    private Integer receiveCount;
}
