package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(title = "群通话房间")
public class GroupCallRoomVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String callId;
    private String groupId;
    private Boolean video;
    private String starterId;
    private Long createTime;
    private List<GroupCallParticipantVO> participants = new ArrayList<>();
}
