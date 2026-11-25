package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName MiddlewareQueryDTO
 * @Description 中间件查询条件
 * @Author Claude Code
 * @Date 2026/5/8
 */
@Data
@Schema(description = "中间件查询条件")
public class MiddlewareQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "组件名称关键词")
    private String componentName;

    @Schema(description = "运行环境")
    private String environment;

    @Schema(description = "运行状态", example = "在线/离线")
    private String status;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    private Integer pageSize = 10;
}
