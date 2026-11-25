package com.xigua.domain.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName QueryLoginLogPageDTO
 * @Description 管理端登录日志分页查询DTO
 * @Author wangjinfei
 * @Date 2026/3/8
 */
@Data
@Schema(description = "管理端登录日志分页查询参数")
public class QueryLoginLogPageDTO extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名/UID（模糊查询）")
    private String username;

    @Schema(description = "操作类型（0：登出；1：密码登录）")
    private Integer loginType;

    @Schema(description = "登录状态（1：成功；0：失败）")
    private Integer loginStatus;

    @Schema(description = "开始时间（格式：yyyy-MM-dd）")
    private String startTime;

    @Schema(description = "结束时间（格式：yyyy-MM-dd）")
    private String endTime;
}
