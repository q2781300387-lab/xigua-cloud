package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(title = "群通话成员")
public class GroupCallParticipantVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;
    private String avatar;
    private Boolean muted;
    private Long joinedAt;
    private Long lastSeen;
}
