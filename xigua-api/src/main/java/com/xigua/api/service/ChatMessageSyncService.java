package com.xigua.api.service;

import com.xigua.domain.vo.ChatMessageSyncVO;

import java.util.List;

/**
 * @ClassName ChatMessageSyncService
 * @Description 聊天消息同步读取接口（供 xigua-search 全量同步到 ES 使用，不携带 mybatis-plus 依赖）
 * @Author wangjinfei
 * @Date 2026/9/29
 */
public interface ChatMessageSyncService {

    /**
     * 统计聊天消息总数
     * @author wangjinfei
     * @date 2026/9/29
     * @return Long
     */
    Long countAll();

    /**
     * 按 id 升序游标分页查询聊天消息
     * @author wangjinfei
     * @date 2026/9/29
     * @param lastId 上一批最后一条消息id，首次传 null
     * @param pageSize 每批数量
     * @return List<ChatMessageSyncVO>
     */
    List<ChatMessageSyncVO> pageByIdAsc(String lastId, Integer pageSize);

    /**
     * 按 createTime + id 双游标分页查询聊天消息，用于增量对账补齐 es 漏掉的数据
     * @author wangjinfei
     * @date 2026/11/12
     * @param afterTime 起始时间（yyyy-MM-dd HH:mm:ss），只查该时间之后的消息
     * @param lastId 上一批最后一条消息id，首次传 null
     * @param pageSize 每批数量
     * @return List<ChatMessageSyncVO>
     */
    List<ChatMessageSyncVO> pageByCreateTimeAsc(String afterTime, String lastId, Integer pageSize);
}
