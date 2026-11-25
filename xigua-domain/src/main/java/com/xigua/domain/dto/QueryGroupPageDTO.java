package com.xigua.domain.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName QueryGroupPageDTO
 * @Description 管理端群组分页查询DTO
 * @Author wangjinfei
 * @Date 2026/3/7
 */
@Data
@Schema(description = "管理端群组分页查询参数")
public class QueryGroupPageDTO extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "群组名称（模糊查询）")
    private String groupName;

    @Schema(description = "群主名称（模糊查询）")
    private String ownerName;

    @Schema(description = "创建时间（格式：yyyy-MM-dd）")
    private String createTime;
}
