package com.xigua.domain.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName NodeOnlineUserVO
 * @Description 节点在线用户VO
 * @Author Claude
 * @Date 2026/03/19
 */
@Data
@Schema(description = "节点在线用户信息")
public class NodeOnlineUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "连接设备（Web/iOS/Android）")
    private String device;

    @Schema(description = "连接时长（秒）")
    private Long duration;

    @Schema(description = "连接时长（格式化：HH:mm:ss）")
    private String durationFormat;

    @Schema(description = "最后心跳时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastHeartbeat;

    @Schema(description = "客户端IP")
    private String clientIp;
}
