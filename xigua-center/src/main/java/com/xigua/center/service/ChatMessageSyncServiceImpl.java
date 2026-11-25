package com.xigua.center.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigua.api.service.ChatMessageSyncService;
import com.xigua.center.mapper.ChatMessageMapper;
import com.xigua.domain.entity.ChatMessage;
import com.xigua.domain.vo.ChatMessageSyncVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName ChatMessageSyncServiceImpl
 * @Description 聊天消息同步读取实现（供 xigua-search 全量同步到 ES 使用）
 * @Author wangjinfei
 * @Date 2026/9/29
 */
@Slf4j
@Service
@DubboService
public class ChatMessageSyncServiceImpl implements ChatMessageSyncService {
    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Override
    public Long countAll() {
        return chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getDelFlag, 0)
        );
    }

    @Override
    public List<ChatMessageSyncVO> pageByIdAsc(String lastId, Integer pageSize) {
        int size = (pageSize == null || pageSize <= 0) ? 500 : pageSize;
        return chatMessageMapper.pageByIdAsc(lastId, size);
    }

    @Override
    public List<ChatMessageSyncVO> pageByCreateTimeAsc(String afterTime, String lastId, Integer pageSize) {
        int size = (pageSize == null || pageSize <= 0) ? 500 : pageSize;
        // 起始时间缺省时退化成按 id 游标全量扫，避免拿 null 直接拼 SQL
        String after = (afterTime == null || afterTime.isBlank()) ? "1970-01-01 00:00:00" : afterTime;
        return chatMessageMapper.pageByCreateTimeAsc(after, lastId, size);
    }
}
