package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(title = "入群申请请求")
public class GroupJoinRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String groupId;

    @Schema(description = "申请理由，可不填")
    private String reason;
}
