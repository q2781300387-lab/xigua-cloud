package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 朋友圈动态展示对象（带发布人、点赞、评论）
 */
@Data
@Schema(description = "朋友圈动态")
public class MomentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String userId;

    @Schema(description = "发布人昵称")
    private String nickname;

    @Schema(description = "发布人头像")
    private String avatar;

    private String content;

    private List<String> images;

    private String location;

    @Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss")
    private String createTime;

    private Integer likeCount;

    private Integer commentCount;

    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    @Schema(description = "点赞人昵称列表")
    private List<String> likes;

    private List<MomentCommentVO> comments;
}
