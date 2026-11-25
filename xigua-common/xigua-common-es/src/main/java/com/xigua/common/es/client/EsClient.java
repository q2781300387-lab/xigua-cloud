package com.xigua.common.es.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName EsClient
 * @Description Elasticsearch 客户端封装，提供索引与文档的常用操作
 * @Author wangjinfei
 * @Date 2026/9/29
 */
@Slf4j
@Component
public class EsClient {

    @Autowired
    private ElasticsearchClient client;

    /**
     * 判断索引是否存在
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @return boolean
     */
    public boolean existsIndex(String index) {
        try {
            return client.indices().exists(e -> e.index(index)).value();
        } catch (IOException e) {
            log.error("判断索引是否存在失败：{}", index, e);
            return false;
        }
    }

    /**
     * 创建索引
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @return boolean
     */
    public boolean createIndex(String index) {
        try {
            if (existsIndex(index)) {
                return true;
            }
            return client.indices().create(c -> c.index(index)).acknowledged();
        } catch (IOException e) {
            log.error("创建索引失败：{}", index, e);
            return false;
        }
    }

    /**
     * 创建索引（自定义 mappings/settings，原始 json 格式，与 ES REST API PUT /{index} 请求体一致）
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @param bodyJson
     * @return boolean
     */
    public boolean createIndex(String index, String bodyJson) {
        try {
            if (existsIndex(index)) {
                return true;
            }
            CreateIndexRequest request = CreateIndexRequest.of(c -> c
                    .index(index)
                    .withJson(new StringReader(bodyJson)));
            return client.indices().create(request).acknowledged();
        } catch (IOException e) {
            log.error("创建索引失败：{}", index, e);
            return false;
        }
    }

    /**
     * 删除索引
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @return boolean
     */
    public boolean deleteIndex(String index) {
        try {
            if (!existsIndex(index)) {
                return true;
            }
            return client.indices().delete(d -> d.index(index)).acknowledged();
        } catch (IOException e) {
            log.error("删除索引失败：{}", index, e);
            return false;
        }
    }

    /**
     * 新增或更新文档
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @param id
     * @param document
     * @return boolean
     */
    public <T> boolean save(String index, String id, T document) {
        try {
            Result result = client.index(i -> i.index(index).id(id).document(document)).result();
            return result == Result.Created || result == Result.Updated;
        } catch (IOException e) {
            log.error("保存文档失败：index={}，id={}", index, id, e);
            return false;
        }
    }

    /**
     * 根据 id 获取文档
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @param id
     * @param clazz
     * @return T
     */
    public <T> T get(String index, String id, Class<T> clazz) {
        try {
            GetResponse<T> response = client.get(g -> g.index(index).id(id), clazz);
            return response.found() ? response.source() : null;
        } catch (IOException e) {
            log.error("获取文档失败：index={}，id={}", index, id, e);
            return null;
        }
    }

    /**
     * 根据 id 删除文档
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @param id
     * @return boolean
     */
    public boolean delete(String index, String id) {
        try {
            Result result = client.delete(d -> d.index(index).id(id)).result();
            return result == Result.Deleted || result == Result.NotFound;
        } catch (IOException e) {
            log.error("删除文档失败：index={}，id={}", index, id, e);
            return false;
        }
    }

    /**
     * 批量新增或更新文档
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @param documents key 为文档 id，value 为文档内容
     * @return boolean
     */
    public <T> boolean bulkSave(String index, Map<String, T> documents) {
        if (MapUtils.isEmpty(documents)) {
            return true;
        }

        try {
            BulkRequest.Builder builder = new BulkRequest.Builder();
            documents.forEach((id, doc) -> builder.operations(op -> op
                    .index(idx -> idx.index(index).id(id).document(doc))));

            BulkResponse response = client.bulk(builder.build());
            if (response.errors()) {
                log.error("批量保存文档存在失败项：index={}", index);
            }
            return !response.errors();
        } catch (IOException e) {
            log.error("批量保存文档失败：index={}", index, e);
            return false;
        }
    }

    /**
     * 按字段做 match 关键词搜索
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @param field
     * @param keyword
     * @param clazz
     * @return List<T>
     */
    public <T> List<T> search(String index, String field, String keyword, Class<T> clazz) {
        try {
            SearchResponse<T> response = client.search(s -> s
                    .index(index)
                    .query(q -> q.match(m -> m.field(field).query(keyword))), clazz);

            List<T> result = new ArrayList<>();
            for (Hit<T> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    result.add(hit.source());
                }
            }
            return result;
        } catch (IOException e) {
            log.error("搜索文档失败：index={}，field={}，keyword={}", index, field, keyword, e);
            return List.of();
        }
    }

    /**
     * 自定义 query 分页搜索
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @param query 查询条件（bool/match/term 等组合）
     * @param from 偏移量
     * @param size 每页数量
     * @param clazz
     * @return EsPage<T>
     */
    public <T> EsPage<T> search(String index, Query query, int from, int size, Class<T> clazz) {
        return search(index, query, from, size, null, null, clazz);
    }

    /**
     * 自定义 query 分页搜索，支持排序
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @param query 查询条件（bool/match/term 等组合）
     * @param from 偏移量
     * @param size 每页数量
     * @param sortField 排序字段，为空则不排序（默认按相关度）
     * @param sortOrder 排序方向，默认 Desc
     * @param clazz
     * @return EsPage<T>
     */
    public <T> EsPage<T> search(String index, Query query, int from, int size,
                                 String sortField, SortOrder sortOrder, Class<T> clazz) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder()
                    .index(index)
                    .query(query)
                    .from(from)
                    .size(size)
                    .trackTotalHits(t -> t.enabled(true));

            if (StringUtils.isNotBlank(sortField)) {
                SortOrder order = sortOrder != null ? sortOrder : SortOrder.Desc;
                builder.sort(so -> so.field(f -> f.field(sortField).order(order)));
            }

            SearchResponse<T> response = client.search(builder.build(), clazz);

            List<T> rows = new ArrayList<>();
            for (Hit<T> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    rows.add(hit.source());
                }
            }

            TotalHits totalHits = response.hits().total();
            long total = totalHits != null ? totalHits.value() : rows.size();
            return new EsPage<>(rows, total);
        } catch (IOException e) {
            log.error("分页搜索失败：index={}", index, e);
            return new EsPage<>(List.of(), 0L);
        }
    }

    /**
     * 自定义 query 分页搜索，并对指定字段做高亮
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @param query 查询条件（bool/match/term 等组合）
     * @param from 偏移量
     * @param size 每页数量
     * @param highlightField 需要高亮的字段
     * @param preTag 高亮前缀标签，如 "<em>"
     * @param postTag 高亮后缀标签，如 "</em>"
     * @param sortField 排序字段，为空则不排序（默认按相关度）
     * @param sortOrder 排序方向，默认 Desc
     * @param clazz
     * @return EsPage<EsHit<T>>
     */
    public <T> EsPage<EsHit<T>> searchWithHighlight(String index, Query query, int from, int size,
                                                      String highlightField, String preTag, String postTag,
                                                      String sortField, SortOrder sortOrder, Class<T> clazz) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder()
                    .index(index)
                    .query(query)
                    .from(from)
                    .size(size)
                    .trackTotalHits(t -> t.enabled(true))
                    .highlight(h -> h.fields(highlightField, f -> f.preTags(preTag).postTags(postTag)));

            if (StringUtils.isNotBlank(sortField)) {
                SortOrder order = sortOrder != null ? sortOrder : SortOrder.Desc;
                builder.sort(so -> so.field(f -> f.field(sortField).order(order)));
            }

            SearchResponse<T> response = client.search(builder.build(), clazz);

            List<EsHit<T>> rows = new ArrayList<>();
            for (Hit<T> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    rows.add(new EsHit<>(hit.source(), hit.highlight()));
                }
            }

            TotalHits totalHits = response.hits().total();
            long total = totalHits != null ? totalHits.value() : rows.size();
            return new EsPage<>(rows, total);
        } catch (IOException e) {
            log.error("高亮搜索失败：index={}", index, e);
            return new EsPage<>(List.of(), 0L);
        }
    }

    /**
     * 统计索引文档数量
     * @author wangjinfei
     * @date 2026/9/29
     * @param index
     * @return long
     */
    public long count(String index) {
        try {
            return client.count(c -> c.index(index)).count();
        } catch (IOException e) {
            log.error("统计文档数量失败：index={}", index, e);
            return 0L;
        }
    }

    /**
     * 获取原始 ElasticsearchClient 实例，用于复杂查询场景
     * @author wangjinfei
     * @date 2026/9/29
     * @return ElasticsearchClient
     */
    public ElasticsearchClient getClient() {
        return client;
    }
}
