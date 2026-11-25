package com.xigua.client.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 群投票选择记录
 */
@Data
@TableName("xg_group_vote_record")
public class GroupVoteRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private String id;

    /**
     * 投票id
     */
    private Long voteId;

    /**
     * 投票人
     */
    private Long userId;

    /**
     * 选项下标，从 0 开始
     */
    private Integer optionIndex;

    /**
     * 删除标记：投票记录不做逻辑删除，取消/改投直接物理删除，
     * 否则唯一键 uk_vote_user_option 会拦住用户取消后重新投票。
     */
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
