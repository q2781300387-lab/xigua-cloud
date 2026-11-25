package com.xigua.common.es.client;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @ClassName EsHit
 * @Description 带高亮片段的搜索命中结果
 * @Author wangjinfei
 * @Date 2026/9/29
 */
@Data
@AllArgsConstructor
public class EsHit<T> {
    /**
     * 原始文档
     */
    private T source;

    /**
     * 高亮片段，key 为字段名，value 为该字段的高亮片段列表
     */
    private Map<String, List<String>> highlights;
}
