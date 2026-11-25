package com.xigua.domain.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName NodeLogVO
 * @Description 节点日志VO
 * @Author Claude
 * @Date 2026/03/18
 */
@Data
@Schema(description = "节点日志信息")
public class NodeLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    private String id;

    @Schema(description = "节点ID")
    private String nodeId;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点编码")
    private String nodeCode;

    @Schema(description = "启动时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "下线时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "状态（0：下线；1：在线）")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "运行时长（秒）")
    private Long duration;

    @Schema(description = "运行时长格式化（如：2小时15分）")
    private String durationFormat;

    @Schema(description = "操作人ID")
    private String operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
