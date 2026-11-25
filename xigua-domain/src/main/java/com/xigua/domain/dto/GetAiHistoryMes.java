package com.xigua.domain.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName GetHistoryMes
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/25 10:15
 */
@Data
@Schema(description = "查询AI历史消息请求")
public class GetAiHistoryMes extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "会话ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sessionId;
}
