package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @ClassName ActiveGroupRankItemVO
 * @Description 活跃群组排行单项
 * @Author wangjinfei
 * @Date 2026/3/9
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "活跃群组排行单项")
public class ActiveGroupRankItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "群组ID")
    private String groupId;

    @Schema(description = "群组名称")
    private String groupName;

    @Schema(description = "成员数量")
    private Integer memberCount;

    @Schema(description = "成员数量（格式化显示，如12.4k）")
    private String memberCountDisplay;
}
