package com.xigua.domain.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName NodeDetailVO
 * @Description 节点详情VO
 * @Author Claude
 * @Date 2026/03/19
 */
@Data
@Schema(description = "节点详情信息")
public class NodeDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "节点ID")
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

    @Schema(description = "在线状态（0：离线；1：在线）")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "启动时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "连接人数")
    private Integer connectionCount;

    @Schema(description = "CPU使用率（百分比）")
    private Double cpuUsage;

    @Schema(description = "内存使用率（百分比）")
    private Double memoryUsage;

    @Schema(description = "消息吞吐（TPS）")
    private Integer tps;

    @Schema(description = "创建时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
