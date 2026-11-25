package com.xigua.domain.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName QueryBotPageDTO
 * @Description 管理端机器人分页查询DTO
 * @Author wangjinfei
 * @Date 2026/3/29
 */
@Data
@Schema(description = "管理端机器人分页查询参数")
public class QueryBotPageDTO extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "机器人名称（模糊查询）")
    private String name;

    @Schema(description = "所属用户ID")
    private String createBy;
}
