package com.xigua.domain.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName QueryUserPageDTO
 * @Description 管理端用户分页查询DTO
 * @Author wangjinfei
 * @Date 2026/2/1 17:13
 */
@Data
@Schema(description = "管理端用户分页查询参数")
public class QueryUserPageDTO extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名（模糊查询）")
    private String username;

    @Schema(description = "创建时间（格式：yyyy-MM-dd）")
    private String createTime;
}
