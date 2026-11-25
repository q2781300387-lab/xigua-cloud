package com.xigua.ai.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @ClassName RoleType
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/24 20:17
 */
@Getter
@ToString
@AllArgsConstructor
@Schema(description = "角色类型")
public enum RoleType {

    @Schema(description = "用户")
    USER("user", "用户"),

    @Schema(description = "AI助手")
    ASSISTANT("assistant", "ai"),

    @Schema(description = "系统")
    SYSTEM("system", "系统提示词"),
    ;

    private final String roleName;
    private final String desc;
}
