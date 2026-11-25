package com.xigua.client.job;

import com.xigua.api.service.NodeClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @ClassName NodeSystemInfoJob
 * @Description 节点系统信息定时任务 - 每3分钟采集一次系统信息存入Redis
 * @Author wangjinfei
 * @Date 2026/3/18
 */
@Slf4j
@Component
public class NodeSystemInfoJob {

    @Autowired
    private NodeClientService nodeClientService;

    /**
     * 每3分钟执行一次系统信息采集
     * initialDelay: 启动后30秒开始首次执行
     * fixedRate: 固定频率执行（180000ms = 3分钟）
     */
    @Scheduled(initialDelay = 30000, fixedRate = 180000)
    public void collectSystemInfo() {
        try {
            nodeClientService.refreshSystemInfo();
        } catch (Exception e) {
            log.error("节点系统信息采集失败", e);
        }
    }
}
