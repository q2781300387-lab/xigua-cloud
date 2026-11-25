package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName SearchPageVO
 * @Description 搜索分页返回对象（字段与 BasePageVO 保持一致，供不依赖 mybatis-plus 的服务使用）
 * @Author wangjinfei
 * @Date 2026/9/29
 */
@Data
@Schema(title = "搜索分页返回对象")
public class SearchPageVO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页数", required = true)
    private Integer pageNum;

    @Schema(description = "每页显示个数", required = true)
    private Integer pageSize;

    @Schema(description = "总页数", required = true)
    private Long totalPage;

    @Schema(description = "总记录数", required = true)
    private Long totalCount;

    @Schema(description = "结果列表", required = true)
    private List<T> rows;
}
