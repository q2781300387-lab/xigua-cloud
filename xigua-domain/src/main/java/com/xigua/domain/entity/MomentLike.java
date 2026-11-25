package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 朋友圈点赞
 */
@Data
@TableName("xg_moment_like")
public class MomentLike implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    private String momentId;

    private String userId;

    private LocalDateTime createTime;
}
