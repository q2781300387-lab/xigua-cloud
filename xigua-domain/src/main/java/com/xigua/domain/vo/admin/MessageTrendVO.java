package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName MessageTrendVO
 * @Description 消息发送趋势统计
 * @Author wangjinfei
 * @Date 2026/3/9
 */
@Data
@Schema(description = "消息发送趋势统计")
public class MessageTrendVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "统计周期说明")
    private String period;

    @Schema(description = "消息趋势数据列表")
    private List<MessageTrendItemVO> trendData;
}
