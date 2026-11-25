package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName SaveNodeDTO
 * @Description 保存节点DTO
 * @Author Claude
 * @Date 2026/03/18
 */
@Data
@Schema(description = "保存节点参数")
public class SaveNodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "节点ID（更新时必填）")
    private String id;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点编码")
    private String nodeCode;

    @Schema(description = "内网IP")
    private String innerIp;

    @Schema(description = "外网IP")
    private String outerIp;

    @Schema(description = "地区")
    private String region;

    @Schema(description = "备注")
    private String remark;
}
