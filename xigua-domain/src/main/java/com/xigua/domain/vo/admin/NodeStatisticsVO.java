package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName NodeStatisticsVO
 * @Description 节点统计VO
 * @Author Claude
 * @Date 2026/03/19
 */
@Data
@Schema(description = "节点统计信息")
public class NodeStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "总节点数")
    private Long totalNodes;

    @Schema(description = "实时总连接数")
    private Long totalConnections;

    @Schema(description = "在线节点数")
    private Long onlineNodes;

    @Schema(description = "离线节点数")
    private Long offlineNodes;
}
