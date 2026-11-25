package com.xigua.domain.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName QueryNodeLogPageDTO
 * @Description 节点日志分页查询DTO
 * @Author Claude
 * @Date 2026/03/18
 */
@Data
@Schema(description = "节点日志分页查询参数")
public class QueryNodeLogPageDTO extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "节点ID")
    private String nodeId;

    @Schema(description = "状态（0：下线；1：在线）")
    private Integer status;

    @Schema(description = "开始时间（格式：yyyy-MM-dd）")
    private String startTime;

    @Schema(description = "结束时间（格式：yyyy-MM-dd）")
    private String endTime;
}
