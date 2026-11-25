package com.xigua.client.listener;

import com.xigua.api.service.NodeClientService;
import com.xigua.api.service.admin.NodeService;
import com.xigua.domain.bo.NodeSystemInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @ClassName ApplicationStartupListener
 * @Description 应用启动监听器，在服务启动完成后执行初始化任务
 * @Author Claude
 * @Date 2026/03/19
 */
@Slf4j
@Component
public class ApplicationStartupListener {

    @Autowired
    private NodeClientService nodeClientService;
    @DubboReference
    private NodeService nodeService;

    /**
     * 应用启动开始自动刷新节点系统信息
     */
    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationReady() {
        log.info("应用启动完成，开始刷新节点系统信息...");
        try {
            NodeSystemInfo nodeSystemInfo = nodeClientService.refreshSystemInfo();
            if(nodeSystemInfo != null) {
                if(StringUtils.isNotEmpty(nodeSystemInfo.getNodeIp())){
                    nodeService.online(nodeSystemInfo.getNodeIp());
                }
            }
            log.info("节点系统信息刷新成功");
        } catch (Exception e) {
            log.error("节点系统信息刷新失败", e);
        }
    }
}
