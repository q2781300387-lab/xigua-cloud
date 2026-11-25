package com.xigua.center.job;

import com.xigua.center.service.MessageArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 消息冷热分层定时任务：每天凌晨触发一次归档。
 */
@Slf4j
@Component
public class MessageArchiveJob {

    @Autowired
    private MessageArchiveService archiveService;

    @Value("${xigua.message.archive-days:180}")
    private int archiveDays;

    @Scheduled(cron = "0 30 3 * * ?")
    public void scheduledArchive() {
        try {
            archiveService.archive(archiveDays);
        } catch (Exception e) {
            log.error("消息归档任务失败", e);
        }
    }
}
