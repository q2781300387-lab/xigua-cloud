package com.xigua.domain.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName QueryOnlineUserPageDTO
 * @Description 管理端在线用户分页查询DTO
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Data
@Schema(description = "管理端在线用户分页查询参数")
public class QueryOnlineUserPageDTO extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名")
    private String username;
}
