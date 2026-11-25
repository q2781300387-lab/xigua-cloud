package com.xigua.domain.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName QueryNodeOnlineUserDTO
 * @Description 查询节点在线用户DTO
 * @Author Claude
 * @Date 2026/03/19
 */
@Data
@Schema(description = "查询节点在线用户参数")
public class QueryNodeOnlineUserDTO extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "节点ID")
    private String nodeId;

    @Schema(description = "搜索关键字（用户ID或昵称）")
    private String keyword;
}
