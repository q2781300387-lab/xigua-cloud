package com.xigua.client.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 群投票实体
 */
@Data
@TableName("xg_group_vote")
public class GroupVote implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private String id;

    /**
     * 所属群id
     */
    private Long groupId;

    /**
     * 发起人
     */
    private Long creatorId;

    /**
     * 投票标题
     */
    private String title;

    /**
     * 选项，JSON 数组文本
     */
    private String options;

    /**
     * 是否多选（0单选 1多选）
     */
    private Integer multi;

    /**
     * 状态（0进行中 1已结束）
     */
    private Integer status;

    @TableLogic
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
