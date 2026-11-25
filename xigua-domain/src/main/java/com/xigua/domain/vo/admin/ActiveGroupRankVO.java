package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName ActiveGroupRankVO
 * @Description 活跃群组排行
 * @Author wangjinfei
 * @Date 2026/3/9
 */
@Data
@Schema(description = "活跃群组排行")
public class ActiveGroupRankVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "排行榜标题")
    private String title;

    @Schema(description = "排行数据列表（按成员数降序，前5名）")
    private List<ActiveGroupRankItemVO> rankList;
}
