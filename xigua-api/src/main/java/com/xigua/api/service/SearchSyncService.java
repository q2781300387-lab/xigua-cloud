package com.xigua.api.service;

import com.xigua.domain.vo.ChatMessageSyncVO;

/**
 * @ClassName SearchSyncService
 * @Description 搜索服务同步接口（由 xigua-search 实现，供其他服务触发数据同步到 ES）
 * @Author wangjinfei
 * @Date 2026/9/29
 */
public interface SearchSyncService {

    /**
     * 全量同步历史聊天记录 mysql -> es
     * @author wangjinfei
     * @date 2026/9/29
     * @return Long 同步的消息数量
     */
    Long syncChatMessage();

    /**
     * 实时写入单条聊天消息到 es（消息发送时同步调用，供搜索功能使用）
     * @author wangjinfei
     * @date 2026/9/29
     * @param message
     * @return Boolean
     */
    Boolean indexChatMessage(ChatMessageSyncVO message);

    /**
     * 增量对账：从 es 里已索引的最新消息时间往后补齐缺失的消息
     * <p>消息发送时的实时写入是异步的，xigua-search 未启动或 es 不可用时会被静默丢弃；
     * 定时对账用来把这类漏掉的消息补回索引。</p>
     * @author wangjinfei
     * @date 2026/11/12
     * @param sinceTime 起始时间（yyyy-MM-dd HH:mm:ss），为空则从 es 中最新一条记录开始
     * @return Long 本次写入 es 的消息数量（含重叠窗口内的重复写入）
     */
    Long reconcileChatMessage(String sinceTime);

    /**
     * 索引中最新一条聊天消息的时间，用于增量对账的水位线
     * @author wangjinfei
     * @date 2026/11/12
     * @return String yyyy-MM-dd HH:mm:ss，索引为空时返回 null
     */
    String latestIndexedCreateTime();
}
