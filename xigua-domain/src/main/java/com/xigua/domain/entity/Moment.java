package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 朋友圈动态
 */
@Data
@TableName("xg_moment")
@Schema(description = "朋友圈动态")
public class Moment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    @Schema(description = "发布人id")
    private String userId;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "图片地址，多个用 | 分隔")
    private String images;

    @Schema(description = "地点")
    private String location;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "是否删除 0否 1是")
    private Integer delFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
