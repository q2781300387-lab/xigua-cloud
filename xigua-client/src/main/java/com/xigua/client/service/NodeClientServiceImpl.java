package com.xigua.client.service;

import com.alibaba.fastjson2.JSON;
import com.xigua.api.service.NodeClientService;
import com.xigua.common.core.util.NetUtil;
import com.xigua.common.core.util.SystemInfoUtil;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.bo.NodeSystemInfo;
import com.xigua.domain.enums.RedisEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ClassName NodeSystemServiceImpl
 * @Description 节点系统信息服务实现
 * @Author wangjinfei
 * @Date 2026/3/18
 */
@Slf4j
@Service
@DubboService
public class NodeClientServiceImpl implements NodeClientService {

    private static final long REDIS_KEY_TTL = 300;

    @Autowired
    private RedisClient redisClient;

    @Override
    public NodeSystemInfo refreshSystemInfo() {
        try {
            // 获取本机IP
            String ip = NetUtil.getLocalAddress();

            // 获取系统信息
            double cpuUsage = SystemInfoUtil.getCpuUsage();
            double memoryUsage = SystemInfoUtil.getMemoryUsage();
            double jvmMemoryUsage = SystemInfoUtil.getJvmMemoryUsage();
            int cpuCores = SystemInfoUtil.getCpuCores();
            long totalMemory = SystemInfoUtil.getTotalMemory();
            String osInfo = SystemInfoUtil.getOsInfo();
            String cpuInfo = SystemInfoUtil.getCpuInfo();
            String memoryInfo = SystemInfoUtil.getMemoryInfo();
            String jvmMemoryInfo = SystemInfoUtil.getJvmMemoryInfo();

            // 获取网络延迟（节点到公网的延迟）
            long networkLatency = NetUtil.getCurrentNetworkLatency();

            // 构建系统信息对象
            NodeSystemInfo systemInfo = new NodeSystemInfo();
            systemInfo.setNodeIp(ip);
            systemInfo.setCpuUsage(cpuUsage);
            systemInfo.setMemoryUsage(memoryUsage);
            systemInfo.setJvmMemoryUsage(jvmMemoryUsage);
            systemInfo.setCpuCores(cpuCores);
            systemInfo.setTotalMemory(totalMemory);
            systemInfo.setOsInfo(osInfo);
            systemInfo.setCpuInfo(cpuInfo);
            systemInfo.setMemoryInfo(memoryInfo);
            systemInfo.setJvmMemoryInfo(jvmMemoryInfo);
            systemInfo.setNetworkLatency(networkLatency);
            systemInfo.setTimestamp(System.currentTimeMillis());

            // 存入Redis，设置TTL
            String redisKey = RedisEnum.CLIENT_NODE_SYSTEM.getKey() + ip;
            String jsonValue = JSON.toJSONString(systemInfo);
            redisClient.set(redisKey, jsonValue, REDIS_KEY_TTL);

            log.info("手动刷新节点系统信息成功: ip={}, cpu={}%, memory={}%",
                    ip, String.format("%.2f", cpuUsage), String.format("%.2f", memoryUsage));
            return systemInfo;
        } catch (Exception e) {
            log.error("手动刷新节点系统信息失败", e);
            return null;
        }
    }
}
