package com.xigua.common.mybatis.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName BasePageVO
 * @Description TODO 基本分页返回vo对象
 * @Author wangjinfei
 * @Date 2024/6/18 20:43
 */
@Schema(title = "分页返回vo对象")
@Data
public class BasePageVO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 当前页数
     */
    @Schema(description = "当前页数", required = true)
    private Integer pageNum;

    /**
     * 每页显示个数
     */
    @Schema(description = "每页显示个数", required = true)
    private Integer pageSize;

    /**
     * 总页数
     */
    @Schema(description = "总页数", required = true)
    private Long totalPage;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数", required = true)
    private Long totalCount;

    /**
     * 结果列表
     */
    @Schema(description = "结果列表", required = true)
    private List<T> rows;
}
