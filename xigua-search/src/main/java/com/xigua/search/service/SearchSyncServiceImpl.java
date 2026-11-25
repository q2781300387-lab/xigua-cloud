package com.xigua.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.xigua.api.service.ChatMessageSyncService;
import com.xigua.api.service.SearchSyncService;
import com.xigua.common.es.client.EsClient;
import com.xigua.domain.vo.ChatMessageSyncVO;
import com.xigua.search.vo.ChatMessageIndexDoc;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName SearchSyncServiceImpl
 * @Description 搜索服务同步实现：全量 / 实时增量 / 定时对账三条路径写入 ES
 * @Author wangjinfei
 * @Date 2026/11/12
 */
@Slf4j
@Service
@DubboService
public class SearchSyncServiceImpl implements SearchSyncService {

    private static final String CHAT_MESSAGE_INDEX = "chat_message";
    private static final int PAGE_SIZE = 500;

    /**
     * 增量对账的重叠窗口（秒）。
     * 水位线取自 ES 里最新一条消息的时间，消息的 createTime 在 center 落库时生成，
     * 而索引写入是异步的，两条消息的写入顺序与 createTime 顺序可能相反，
     * 因此每次往前多回看一段，避免边界上的消息被永久跳过。
     */
    private static final long RECONCILE_OVERLAP_SECONDS = 300L;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * message 字段使用 smartcn 中文分词（ES 发行版内置，无需额外 IK 插件）；
     * senderId/receiverId 用 keyword 精确匹配；createTime 用 date，格式与 messageDbHandle 写入时保持一致
     */
    private static final String CHAT_MESSAGE_INDEX_MAPPING = "{"
            + "\"mappings\":{"
            + "\"properties\":{"
            + "\"id\":{\"type\":\"keyword\"},"
            + "\"senderId\":{\"type\":\"keyword\"},"
            + "\"receiverId\":{\"type\":\"keyword\"},"
            + "\"message\":{\"type\":\"text\",\"analyzer\":\"smartcn\",\"search_analyzer\":\"smartcn\"},"
            + "\"messageRaw\":{\"type\":\"keyword\",\"ignore_above\":256},"
            + "\"chatType\":{\"type\":\"integer\"},"
            + "\"messageType\":{\"type\":\"integer\"},"
            + "\"createTime\":{\"type\":\"date\",\"format\":\"yyyy-MM-dd HH:mm:ss\"}"
            + "}"
            + "}"
            + "}";

    @DubboReference
    private ChatMessageSyncService chatMessageSyncService;

    @Autowired
    private EsClient esClient;

    @Override
    public Long syncChatMessage() {
        esClient.createIndex(CHAT_MESSAGE_INDEX, CHAT_MESSAGE_INDEX_MAPPING);

        String lastId = null;
        long total = 0;

        while (true) {
            List<ChatMessageSyncVO> page = chatMessageSyncService.pageByIdAsc(lastId, PAGE_SIZE);
            if (CollectionUtils.isEmpty(page)) {
                break;
            }

            Map<String, ChatMessageSyncVO> documents = new HashMap<>();
            for (ChatMessageSyncVO vo : page) {
                vo.setMessageRaw(vo.getMessage());
                documents.put(vo.getId(), vo);
            }
            esClient.bulkSave(CHAT_MESSAGE_INDEX, documents);

            total += page.size();
            lastId = page.get(page.size() - 1).getId();
            log.info("聊天记录同步中，已同步 {} 条，游标 lastId={}", total, lastId);

            if (page.size() < PAGE_SIZE) {
                break;
            }
        }

        log.info("聊天记录全量同步完成，共同步 {} 条", total);
        return total;
    }

    @Override
    public Boolean indexChatMessage(ChatMessageSyncVO message) {
        if (message == null || StringUtils.isBlank(message.getId())) {
            return false;
        }
        message.setMessageRaw(message.getMessage());
        return esClient.save(CHAT_MESSAGE_INDEX, message.getId(), message);
    }

    @Override
    public Long reconcileChatMessage(String sinceTime) {
        esClient.createIndex(CHAT_MESSAGE_INDEX, CHAT_MESSAGE_INDEX_MAPPING);

        String anchor = StringUtils.isNotBlank(sinceTime) ? sinceTime : latestIndexedCreateTime();
        if (StringUtils.isBlank(anchor)) {
            // 索引还是空的，直接走全量，比逐段往前找更快
            log.info("聊天记录索引为空，增量对账退化为全量同步");
            return syncChatMessage();
        }

        String afterTime = anchor;
        try {
            LocalDateTime start = LocalDateTime.parse(anchor, DATE_TIME_FORMATTER)
                    .minusSeconds(RECONCILE_OVERLAP_SECONDS);
            afterTime = start.format(DATE_TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("增量对账水位线格式异常，按原值使用：{}", anchor);
        }

        String lastId = null;
        long written = 0;
        while (true) {
            List<ChatMessageSyncVO> page = chatMessageSyncService.pageByCreateTimeAsc(afterTime, lastId, PAGE_SIZE);
            if (CollectionUtils.isEmpty(page)) {
                break;
            }

            Map<String, ChatMessageSyncVO> documents = new HashMap<>();
            for (ChatMessageSyncVO vo : page) {
                vo.setMessageRaw(vo.getMessage());
                documents.put(vo.getId(), vo);
            }
            esClient.bulkSave(CHAT_MESSAGE_INDEX, documents);

            written += page.size();
            lastId = page.get(page.size() - 1).getId();
            if (page.size() < PAGE_SIZE) {
                break;
            }
        }

        log.info("聊天记录增量对账完成，水位线 {}（回看 {} 秒），写入 {} 条", anchor, RECONCILE_OVERLAP_SECONDS, written);
        return written;
    }

    @Override
    public String latestIndexedCreateTime() {
        if (!esClient.existsIndex(CHAT_MESSAGE_INDEX)) {
            return null;
        }
        try {
            SearchResponse<ChatMessageIndexDoc> response = esClient.getClient().search(s -> s
                            .index(CHAT_MESSAGE_INDEX)
                            .size(1)
                            .sort(so -> so.field(f -> f.field("createTime").order(SortOrder.Desc))),
                    ChatMessageIndexDoc.class
            );
            List<Hit<ChatMessageIndexDoc>> hits = response.hits().hits();
            if (CollectionUtils.isEmpty(hits) || hits.get(0).source() == null) {
                return null;
            }
            return hits.get(0).source().getCreateTime();
        } catch (Exception e) {
            log.error("读取聊天消息索引水位线失败", e);
            return null;
        }
    }
}
