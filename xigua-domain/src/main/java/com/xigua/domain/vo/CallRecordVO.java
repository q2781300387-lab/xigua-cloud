package com.xigua.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 单条视频通话记录（来自 xg_chat_message 中 messageType=9 的通话卡片）
 */
@Data
@Schema(title = "通话记录")
public class CallRecordVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息id")
    private String messageId;

    @Schema(description = "对端用户id")
    private String peerId;

    @Schema(description = "对端用户名")
    private String peerName;

    @Schema(description = "对端头像")
    private String peerAvatar;

    @Schema(description = "方向：outgoing 我呼出 / incoming 我呼入")
    private String direction;

    @Schema(description = "结果：completed/rejected/timeout/canceled/failed/busy")
    private String outcome;

    @Schema(description = "通话时长（秒）")
    private Integer duration;

    @Schema(description = "是否未接（仅呼入的 rejected/timeout/busy）")
    private Boolean missed;

    @Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss")
    private String createTime;

    /**
     * 原始消息体，只在后端解析用，不返回给前端
     */
    @JsonIgnore
    private transient String message;
}
