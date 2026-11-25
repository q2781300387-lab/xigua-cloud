package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 入群申请
 */
@Data
@TableName("xg_group_join_request")
@Schema(title = "入群申请")
public class GroupJoinRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private String id;

    private String groupId;

    private String userId;

    private String reason;

    /** 0 待审批；1 已通过；2 已拒绝 */
    private Integer status;

    private String handlerId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
