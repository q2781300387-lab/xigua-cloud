package com.xigua.center.service;

import com.xigua.center.mapper.MessageArchiveMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息冷热分层：把超过保留期的消息搬到归档表。
 * 归档表结构与热表一致，历史消息查询通过 UNION 同时覆盖两张表。
 */
@Slf4j
@Service
public class MessageArchiveService {

    private static final int BATCH_SIZE = 2000;

    @Autowired
    private MessageArchiveMapper archiveMapper;

    public int archive(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(Math.max(days, 1));
        long remaining = archiveMapper.countBefore(cutoff);
        int moved = 0;
        while (remaining > 0) {
            List<String> ids = archiveMapper.selectIdsBefore(cutoff, BATCH_SIZE);
            if (ids.isEmpty()) {
                break;
            }
            moved += moveBatch(ids);
            remaining -= ids.size();
            log.info("消息归档批次完成：本批 {} 条，剩余约 {} 条，cutoff={}", ids.size(), remaining, cutoff);
        }
        log.info("消息归档任务结束：cutoff={}，共迁移 {} 条", cutoff, moved);
        return moved;
    }

    @Transactional(rollbackFor = Exception.class)
    public int moveBatch(List<String> ids) {
        archiveMapper.insertByIds(ids);
        archiveMapper.deleteByIds(ids);
        return ids.size();
    }

    public long countBefore(int days) {
        return archiveMapper.countBefore(LocalDateTime.now().minusDays(Math.max(days, 1)));
    }
}
