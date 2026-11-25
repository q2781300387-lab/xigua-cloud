package com.xigua.common.es.client;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @ClassName EsPage
 * @Description ES 分页查询结果
 * @Author wangjinfei
 * @Date 2026/9/29
 */
@Data
@AllArgsConstructor
public class EsPage<T> {
    /**
     * 当前页数据
     */
    private List<T> rows;

    /**
     * 命中总数
     */
    private long total;
}
