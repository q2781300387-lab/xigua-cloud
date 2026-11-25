package com.xigua.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.ToString;

/**
 * @ClassName NodeStatus
 * @Description 节点状态枚举
 * @Author Claude
 * @Date 2026/03/18
 */
@ToString
@AllArgsConstructor
@Schema(description = "节点状态")
public enum NodeStatus {

    @Schema(description = "下线")
    OFFLINE(0, "下线"),

    @Schema(description = "在线")
    ONLINE(1, "在线");

    final Integer status;

    final String desc;

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据状态值获取枚举
     * @param status 状态值
     * @return NodeStatus
     */
    public static NodeStatus getByStatus(Integer status) {
        for (NodeStatus nodeStatus : NodeStatus.values()) {
            if (nodeStatus.status.equals(status)) {
                return nodeStatus;
            }
        }
        return null;
    }
}
