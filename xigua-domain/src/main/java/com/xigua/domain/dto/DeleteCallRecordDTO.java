package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 删除单条通话记录（按当前用户维度隐藏）
 */
@Data
@Schema(title = "删除通话记录")
public class DeleteCallRecordDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "通话消息id")
    private String messageId;
}
