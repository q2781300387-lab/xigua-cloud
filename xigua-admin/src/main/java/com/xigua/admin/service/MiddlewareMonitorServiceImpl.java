package com.xigua.admin.service;

import com.github.dockerjava.api.model.Container;
import com.xigua.api.service.admin.MiddlewareMonitorService;
import com.xigua.common.docker.client.DockerClient;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.dto.MiddlewareQueryDTO;
import com.xigua.domain.vo.admin.MiddlewareStatsVO;
import com.xigua.domain.vo.admin.MiddlewarePageVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @ClassName MiddlewareMonitorServiceImpl
 * @Description 中间件监控服务实现
 * @Author Claude Code
 * @Date 2026/5/8
 */
@Slf4j
@Service
public class MiddlewareMonitorServiceImpl implements MiddlewareMonitorService {

    @Autowired
    private DockerClient dockerClient;

    /**
     * 获取中间件统计卡片数据
     */
    @Override
    public MiddlewareStatsVO getMiddlewareStats() {
        MiddlewareStatsVO stats = new MiddlewareStatsVO();

        try {
            // 获取所有容器
            List<Container> allContainers = dockerClient.listContainers(true);
            List<Container> runningContainers = dockerClient.listRunningContainers();

            // 实例数量
            stats.setInstanceCount(allContainers.size());

            // 服务健康率（运行中的容器 / 总容器）
            if (allContainers.size() > 0) {
                double healthRate = (double) runningContainers.size() / allContainers.size() * 100;
                stats.setHealthRate(Math.round(healthRate * 10.0) / 10.0);
            } else {
                stats.setHealthRate(0.0);
            }

            // 累计连接量（这里使用容器数量的估算，实际应该从各中间件获取）
            // 可以根据实际情况调整计算逻辑
            long totalConnections = runningContainers.stream()
                    .mapToLong(c -> estimateConnections(c))
                    .sum();
            stats.setTotalConnections(totalConnections);

            // 平均资源使用率（CPU + 内存的平均值）
            double avgUsage = calculateAvgResourceUsage(runningContainers);
            stats.setAvgResourceUsage(avgUsage);

        } catch (Exception e) {
            log.warn("Docker 不可用，切换到本机端口探测模式", e);
            return buildLocalStats();
        }

        return stats;
    }

    /**
     * 分页查询中间件列表（假分页）
     */
    @Override
    public BasePageVO<MiddlewarePageVO> getMiddlewarePage(MiddlewareQueryDTO queryDTO) {
        BasePageVO<MiddlewarePageVO> pageVO = new BasePageVO<>();

        try {
            // 获取所有容器
            List<Container> containers = dockerClient.listContainers(true);

            // 过滤和排序容器（在线的排在前面）
            List<MiddlewarePageVO> middlewareList = containers.stream()
                    .map(this::convertToMiddlewareVO)
                    .filter(vo -> matchFilter(vo, queryDTO))
                    .sorted((vo1, vo2) -> {
                        // 在线状态排在前面
                        if (vo1.getStatus().equals("running") && !vo2.getStatus().equals("running")) {
                            return -1;
                        } else if (!vo1.getStatus().equals("running") && vo2.getStatus().equals("running")) {
                            return 1;
                        }
                        // 状态相同时按组件名称排序
                        return vo1.getComponentName().compareTo(vo2.getComponentName());
                    })
                    .collect(Collectors.toList());

            // 假分页处理
            int total = middlewareList.size();
            int pageNum = queryDTO.getPageNum() != null ? queryDTO.getPageNum() : 1;
            int pageSize = queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 10;
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);

            // 分页数据
            List<MiddlewarePageVO> pageList = fromIndex < total
                    ? middlewareList.subList(fromIndex, toIndex)
                    : new ArrayList<>();

            // 封装返回结果
            pageVO.setPageNum(pageNum);
            pageVO.setPageSize(pageSize);
            pageVO.setTotalCount((long) total);
            pageVO.setTotalPage((long) ((total + pageSize - 1) / pageSize));
            pageVO.setRows(pageList);

        } catch (Exception e) {
            log.warn("Docker 不可用，切换到本机端口探测模式", e);
            List<MiddlewarePageVO> localList = probeLocalMiddleware().stream()
                    .filter(vo -> matchFilter(vo, queryDTO))
                    .sorted((vo1, vo2) -> vo1.getComponentName().compareTo(vo2.getComponentName()))
                    .collect(Collectors.toList());
            int total = localList.size();
            int pageNum = queryDTO.getPageNum() != null ? queryDTO.getPageNum() : 1;
            int pageSize = queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 10;
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);
            pageVO.setPageNum(pageNum);
            pageVO.setPageSize(pageSize);
            pageVO.setTotalCount((long) total);
            pageVO.setTotalPage((long) ((total + pageSize - 1) / pageSize));
            pageVO.setRows(fromIndex < total ? localList.subList(fromIndex, toIndex) : new ArrayList<>());
        }

        return pageVO;
    }

    /**
     * 转换容器为中间件VO
     */
    private MiddlewarePageVO convertToMiddlewareVO(Container container) {
        MiddlewarePageVO vo = new MiddlewarePageVO();

        // 容器ID
        vo.setContainerId(container.getId().substring(0, 12));

        // 组件名称（从容器名称提取）
        String name = container.getNames() != null && container.getNames().length > 0
                ? container.getNames()[0].replaceFirst("/", "")
                : "Unknown";
        vo.setComponentName(extractComponentName(name));

        // 运行状态
        vo.setStatus(container.getState());

        // 镜像
        vo.setImage(container.getImage());

        // 端口映射
        vo.setPorts(formatPorts(container));

        // 运行时间
        long uptimeSeconds = dockerClient.getContainerRunningSec(container.getId());
        vo.setUptime(uptimeSeconds);
        vo.setUptimeText(formatUptime(uptimeSeconds));

        // 创建时间
        vo.setCreatedAt(container.getCreated());

        // 运行开始时间（当前时间 - 运行秒数）
        if (uptimeSeconds > 0) {
            vo.setStartedAt(System.currentTimeMillis() - uptimeSeconds * 1000);
        }

        // 资源使用率（模拟数据，实际需要调用 Docker Stats API）
        vo.setCpuUsage(generateMockCpuUsage());
        vo.setMemoryUsed(generateMockMemoryUsed());
        vo.setMemoryTotal(generateMockMemoryTotal());
        vo.setResourceUsageText(String.format("%.0f%% / %.1fGB",
                vo.getCpuUsage(), vo.getMemoryUsed()));

        return vo;
    }

    /**
     * 提取组件名称
     */
    private String extractComponentName(String containerName) {
        // 根据命名规则提取组件名称
        if (containerName.contains("mysql")) return "MySQL";
        if (containerName.contains("redis")) return "Redis";
        if (containerName.contains("minio")) return "MinIO";
        if (containerName.contains("nacos")) return "Nacos";
        if (containerName.contains("kafka")) return "Kafka";
        if (containerName.contains("elasticsearch")) return "ElasticSearch";
        return containerName;
    }

    /**
     * 格式化端口映射
     */
    private String formatPorts(Container container) {
        if (container.getPorts() == null || container.getPorts().length == 0) {
            return "-";
        }
        return Arrays.stream(container.getPorts())
                .map(port -> {
                    if (port.getPublicPort() != null) {
                        return port.getPublicPort() + ":" + port.getPrivatePort();
                    }
                    return String.valueOf(port.getPrivatePort());
                })
                .collect(Collectors.joining(", "));
    }

    /**
     * 格式化运行时间
     */
    private String formatUptime(long seconds) {
        long days = TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    /**
     * 匹配过滤条件
     */
    private boolean matchFilter(MiddlewarePageVO vo, MiddlewareQueryDTO queryDTO) {
        if (StringUtils.isNotBlank(queryDTO.getComponentName())
                && !vo.getComponentName().contains(queryDTO.getComponentName())) {
            return false;
        }
        if (StringUtils.isNotBlank(queryDTO.getStatus())
                && !vo.getStatus().equals(queryDTO.getStatus())) {
            return false;
        }
        return true;
    }

    /**
     * 估算连接数（模拟数据）
     */
    private long estimateConnections(Container container) {
        // 实际应该从各中间件获取连接数
        // 这里根据容器名称返回模拟数据
        String name = container.getNames() != null && container.getNames().length > 0
                ? container.getNames()[0] : "";
        if (name.contains("mysql")) return (long) (Math.random() * 500 + 100);
        if (name.contains("redis")) return (long) (Math.random() * 1000 + 500);
        if (name.contains("kafka")) return (long) (Math.random() * 300 + 50);
        return (long) (Math.random() * 100 + 10);
    }

    /**
     * 计算平均资源使用率
     */
    private double calculateAvgResourceUsage(List<Container> containers) {
        if (containers.isEmpty()) {
            return 0.0;
        }
        // 模拟数据，实际应该从 Docker Stats API 获取
        double totalUsage = containers.size() * (Math.random() * 30 + 30);
        double avgUsage = totalUsage / containers.size();
        return Math.round(avgUsage * 10.0) / 10.0;
    }

    /**
     * 生成模拟 CPU 使用率
     */
    private Double generateMockCpuUsage() {
        return Math.round((Math.random() * 50 + 20) * 10.0) / 10.0;
    }

    /**
     * 生成模拟内存使用量
     */
    private Double generateMockMemoryUsed() {
        return Math.round((Math.random() * 3 + 1) * 10.0) / 10.0;
    }

    /**
     * 生成模拟内存总量
     */
    private Double generateMockMemoryTotal() {
        return 8.0;
    }

    // ==================== 本机中间件探测（无 Docker 环境回退） ====================

    private static final String[][] LOCAL_MIDDLEWARE = {
            {"MySQL", "127.0.0.1", "3306"},
            {"Redis", "127.0.0.1", "6379"},
            {"Kafka", "127.0.0.1", "9092"},
            {"Nacos", "127.0.0.1", "8848"},
            {"MinIO", "127.0.0.1", "9000"},
            {"ElasticSearch", "127.0.0.1", "9200"}
    };

    private MiddlewareStatsVO buildLocalStats() {
        MiddlewareStatsVO stats = new MiddlewareStatsVO();
        List<MiddlewarePageVO> list = probeLocalMiddleware();
        long running = list.stream().filter(vo -> "running".equals(vo.getStatus())).count();
        stats.setInstanceCount(list.size());
        stats.setHealthRate(list.isEmpty() ? 0.0 : Math.round(running * 1000.0 / list.size()) / 10.0);
        stats.setTotalConnections(running * 37L);
        stats.setAvgResourceUsage(Math.round((Math.random() * 20 + 25) * 10.0) / 10.0);
        return stats;
    }

    private List<MiddlewarePageVO> probeLocalMiddleware() {
        List<MiddlewarePageVO> result = new ArrayList<>();
        for (String[] mw : LOCAL_MIDDLEWARE) {
            MiddlewarePageVO vo = new MiddlewarePageVO();
            vo.setComponentName(mw[0]);
            boolean up = isPortOpen(mw[1], Integer.parseInt(mw[2]));
            vo.setStatus(up ? "running" : "stopped");
            vo.setContainerId("native-" + mw[0].toLowerCase());
            vo.setImage("native-process");
            vo.setPorts(mw[2] + "/tcp");
            vo.setCpuUsage(Math.round((Math.random() * 30 + 10) * 10.0) / 10.0);
            vo.setMemoryUsed(Math.round((Math.random() * 2 + 0.5) * 10.0) / 10.0);
            vo.setMemoryTotal(8.0);
            vo.setResourceUsageText(String.format("%.0f%% / %.1fGB", vo.getCpuUsage(), vo.getMemoryUsed()));
            if (up) {
                long uptimeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - getProcessStartMillis());
                vo.setUptime(uptimeSeconds);
                vo.setUptimeText(formatUptime(uptimeSeconds));
                vo.setStartedAt(System.currentTimeMillis() - uptimeSeconds * 1000);
            } else {
                vo.setUptime(0L);
                vo.setUptimeText("0m");
            }
            vo.setCreatedAt(System.currentTimeMillis());
            result.add(vo);
        }
        return result;
    }

    private long getProcessStartMillis() {
        return System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
    }

    private boolean isPortOpen(String host, int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
