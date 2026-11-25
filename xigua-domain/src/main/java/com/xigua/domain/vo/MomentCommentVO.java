package com.xigua.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 朋友圈评论展示对象
 */
@Data
public class MomentCommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String userId;

    private String nickname;

    private String text;

    private String createTime;
}
