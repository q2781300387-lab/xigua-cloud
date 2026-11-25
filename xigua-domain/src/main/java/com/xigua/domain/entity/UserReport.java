package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户举报记录，供后台审核。
 */
@Data
@TableName("xg_user_report")
@Schema(title = "用户举报")
public class UserReport implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private String id;

    private String reporterId;

    private String targetId;

    /** user / group / message */
    private String targetType;

    private String reason;

    private String detail;

    /** 0 待处理，1 已处理，2 已驳回 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
