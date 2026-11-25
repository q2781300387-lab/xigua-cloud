package com.xigua.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(title = "入群申请/群搜索结果")
public class GroupJoinRequestVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String groupId;
    private String userId;
    private String reason;
    private Integer status;
    private String handlerId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 群信息 */
    private String groupName;
    private String groupAvatar;
    private String groupDescription;
    private Integer currentMember;
    private String ownerId;

    /** 申请人信息 */
    private String username;

    /** 当前登录人是否已在群内 */
    private Boolean joined;
}
