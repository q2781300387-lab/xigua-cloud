package com.xigua.domain.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName AdminGroupDetailVO
 * @Description 管理端群组详情VO
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Data
@Schema(description = "管理端群组详情信息")
public class AdminGroupDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "群组ID")
    private String id;

    @Schema(description = "群组名称")
    private String groupName;

    @Schema(description = "群头像")
    private String groupAvatar;

    @Schema(description = "群主ID")
    private String ownerId;

    @Schema(description = "群主用户名")
    private String ownerName;

    @Schema(description = "成员人数")
    private Integer memberCount;

    @Schema(description = "状态（0：正常；1：已删除）")
    private Integer status;

    @Schema(description = "全员禁言（0：关闭；1：开启）")
    private Integer muteAll;

    @Schema(description = "群描述")
    private String description;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
