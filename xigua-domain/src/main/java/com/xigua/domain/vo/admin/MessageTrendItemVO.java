package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @ClassName MessageTrendItemVO
 * @Description 消息趋势单点数据
 * @Author wangjinfei
 * @Date 2026/3/9
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息趋势单点数据")
public class MessageTrendItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "时间点（格式：HH:mm）")
    private String timePoint;

    @Schema(description = "消息发送量")
    private Integer messageCount;
}
