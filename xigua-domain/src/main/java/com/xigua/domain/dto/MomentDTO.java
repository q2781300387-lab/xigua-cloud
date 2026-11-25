package com.xigua.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 朋友圈通用请求参数（发布 / 列表 / 点赞 / 评论 / 删除）
 */
@Data
public class MomentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;

    private String momentId;

    /** 评论 id（删除评论时使用） */
    private String commentId;

    private String content;

    private List<String> images;

    private String location;

    private Integer pageNum;

    private Integer pageSize;

    /** 已读时间点（yyyy-MM-dd HH:mm:ss），用于统计未读互动 */
    private String sinceTime;
}
