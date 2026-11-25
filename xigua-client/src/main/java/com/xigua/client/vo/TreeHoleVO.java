package com.xigua.client.vo;

import com.xigua.client.entity.TreeHole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName TreeHoleVO
 * @Description 树洞视图对象（不含发布者信息，保证匿名）
 * @Author xigua
 * @Date 2026/10/16 19:30
 */
@Data
@Schema(title = "树洞视图对象")
public class TreeHoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "树洞id")
    private String id;

    @Schema(description = "树洞内容")
    private String content;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    /**
     * 实体转 VO（剔除发布者身份字段，匿名展示）
     */
    public static TreeHoleVO from(TreeHole th) {
        TreeHoleVO vo = new TreeHoleVO();
        vo.setId(th.getId());
        vo.setContent(th.getContent());
        vo.setLikeCount(th.getLikeCount());
        vo.setCreateTime(th.getCreateTime());
        return vo;
    }
}
