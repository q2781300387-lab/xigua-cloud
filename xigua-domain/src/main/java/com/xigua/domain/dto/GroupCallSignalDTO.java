package com.xigua.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(title = "群通话信令请求")
public class GroupCallSignalDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String groupId;
    private String callId;
    private String targetUserId;
    private String type;
    private Object payload;
}
