package com.xigua.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName BotDetailVO
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/10/8 18:17
 */
@Data
@Schema(description = "机器人详情")
public class BotDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "机器人ID")
    private String id;
    @Schema(description = "机器人名称")
    private String name;
    @Schema(description = "机器人头像")
    private String avatar;
    @Schema(description = "机器人描述")
    private String description;
    @Schema(description = "机器人提示词")
    private String prompt;
    @Schema(description = "创建时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
