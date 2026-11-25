package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(title = "群通话信令")
public class GroupCallSignalVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String fromUserId;
    private String fromUsername;
    private String type;
    private Object payload;
    private Long timestamp;
}
