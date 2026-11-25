package com.xigua.domain.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName QueryNodePageDTO
 * @Description 节点分页查询DTO
 * @Author Claude
 * @Date 2026/03/18
 */
@Data
@Schema(description = "节点分页查询参数")
public class QueryNodePageDTO extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "节点名称（模糊查询）")
    private String nodeName;

    @Schema(description = "节点编码")
    private String nodeCode;

    @Schema(description = "地区（模糊查询）")
    private String region;
}
