package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(title = "审批入群申请请求")
public class GroupJoinHandleDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;

    @Schema(description = "true 通过；false 拒绝")
    private Boolean approve;
}
