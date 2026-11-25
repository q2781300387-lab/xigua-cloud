package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName OpenChatVO
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/12/7 16:38
 */
@Data
@Schema(description = "打开聊天")
public class OpenChatVO extends LastMessageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "排名")
    private Long rank;
}
