package com.xigua.center.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.center.mapper.NodeLogMapper;
import com.xigua.api.service.UserService;
import com.xigua.api.service.admin.NodeLogService;
import com.xigua.api.service.admin.NodeService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.common.mybatis.base.PagePlus;
import com.xigua.domain.dto.QueryNodeLogPageDTO;
import com.xigua.domain.entity.Node;
import com.xigua.domain.entity.NodeLog;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.NodeStatus;
import com.xigua.domain.util.BasePage;
import com.xigua.domain.vo.admin.NodeLogVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName NodeLogServiceImpl
 * @Description 节点日志服务实现
 * @Author Claude
 * @Date 2026/03/18
 */
@Slf4j
@Service
@DubboService
public class NodeLogServiceImpl extends ServiceImpl<NodeLogMapper, NodeLog> implements NodeLogService {

    @Autowired
    private NodeService nodeService;

    @DubboReference
    private UserService userService;

    @Override
    public BasePageVO<NodeLogVO> pageNodeLogs(QueryNodeLogPageDTO dto) {
        Page<NodeLog> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        LambdaQueryWrapper<NodeLog> wrapper = new LambdaQueryWrapper<>();

        // 节点ID查询
        if (StringUtils.isNotEmpty(dto.getNodeId())) {
            wrapper.eq(NodeLog::getNodeId, dto.getNodeId());
        }

        // 状态查询
        if (dto.getStatus() != null) {
            wrapper.eq(NodeLog::getStatus, dto.getStatus());
        }

        // 时间范围查询
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (StringUtils.isNotEmpty(dto.getStartTime())) {
            LocalDate startDate = LocalDate.parse(dto.getStartTime(), formatter);
            LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIN);
            wrapper.ge(NodeLog::getStartTime, startDateTime);
        }
        if (StringUtils.isNotEmpty(dto.getEndTime())) {
            LocalDate endDate = LocalDate.parse(dto.getEndTime(), formatter);
            LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.MAX);
            wrapper.le(NodeLog::getStartTime, endDateTime);
        }

        // 按启动时间倒序
        wrapper.orderByDesc(NodeLog::getStartTime);

        Page<NodeLog> nodeLogPage = baseMapper.selectPage(page, wrapper);

        // 批量查询节点信息
        List<String> nodeIds = nodeLogPage.getRecords().stream()
                .map(NodeLog::getNodeId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Node> nodeMap = nodeIds.isEmpty() ? Map.of() :
                nodeService.listByIds(nodeIds).stream()
                        .collect(Collectors.toMap(Node::getId, node -> node));

        // 批量查询操作人信息
        List<String> createByIds = nodeLogPage.getRecords().stream()
                .map(NodeLog::getCreateBy)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());

        Map<String, User> userMap = createByIds.isEmpty() ? Map.of() :
                userService.listByIds(createByIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user));

        List<NodeLogVO> voList = nodeLogPage.getRecords().stream().map(log -> {
            NodeLogVO vo = new NodeLogVO();
            BeanUtils.copyProperties(log, vo);

            // 设置节点信息
            Node node = nodeMap.get(log.getNodeId());
            if (node != null) {
                vo.setNodeName(node.getNodeName());
                vo.setNodeCode(node.getNodeCode());
            }

            // 设置状态描述
            NodeStatus status = NodeStatus.getByStatus(log.getStatus());
            if (status != null) {
                vo.setStatusDesc(status.getDesc());
            }

            // 计算运行时长
            if (log.getStartTime() != null) {
                LocalDateTime endTime = log.getEndTime() != null ? log.getEndTime() : LocalDateTime.now();
                Duration duration = Duration.between(log.getStartTime(), endTime);
                long seconds = duration.getSeconds();
                vo.setDuration(seconds);
                vo.setDurationFormat(formatDuration(seconds));
            }

            // 设置操作人信息
            if (StringUtils.isNotEmpty(log.getCreateBy())) {
                vo.setOperatorId(log.getCreateBy());
                User user = userMap.get(log.getCreateBy());
                if (user != null) {
                    vo.setOperatorName(user.getUsername());
                }
            }

            return vo;
        }).collect(Collectors.toList());

        PagePlus<NodeLog> pagePlus = PagePlus.fromPage(nodeLogPage);
        return BasePage.getResult(pagePlus, voList);
    }

    @Override
    public NodeLogVO getNodeLogById(String id) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }

        NodeLog log = baseMapper.selectById(id);
        if (log == null) {
            return null;
        }

        NodeLogVO vo = new NodeLogVO();
        BeanUtils.copyProperties(log, vo);

        // 查询节点信息
        Node node = nodeService.getById(log.getNodeId());
        if (node != null) {
            vo.setNodeName(node.getNodeName());
            vo.setNodeCode(node.getNodeCode());
        }

        // 设置状态描述
        NodeStatus status = NodeStatus.getByStatus(log.getStatus());
        if (status != null) {
            vo.setStatusDesc(status.getDesc());
        }

        return vo;
    }

    @Override
    public Boolean recordNodeOnline(Node node) {
        if (node == null) {
            throw new BusinessException("节点信息不能为空");
        }

        // 检查是否有未结束的在线日志，如果有则先结束
        NodeLog activeLog = baseMapper.selectOne(
                new LambdaQueryWrapper<NodeLog>()
                        .eq(NodeLog::getNodeId, node.getId())
                        .eq(NodeLog::getStatus, NodeStatus.ONLINE.getStatus())
                        .orderByDesc(NodeLog::getStartTime)
                        .last("LIMIT 1")
        );

        if (activeLog != null) {
            log.info("节点[{}]已有在线记录，跳过重复上线", node.getNodeName());
            return true;
        }

        // 创建新的上线日志
        NodeLog nodeLog = new NodeLog();
        nodeLog.setNodeId(node.getId());
        nodeLog.setStartTime(LocalDateTime.now());
        nodeLog.setStatus(NodeStatus.ONLINE.getStatus());

        return baseMapper.insert(nodeLog) > 0;
    }

    @Override
    public Boolean recordNodeOffline(Node node) {
        if (node == null) {
            throw new BusinessException("节点信息不能为空");
        }

        // 查找该节点当前在线的日志
        NodeLog activeLog = baseMapper.selectOne(
                new LambdaQueryWrapper<NodeLog>()
                        .eq(NodeLog::getNodeId, node.getId())
                        .eq(NodeLog::getStatus, NodeStatus.ONLINE.getStatus())
                        .orderByDesc(NodeLog::getStartTime)
                        .last("LIMIT 1")
        );

        if (activeLog == null) {
            throw new BusinessException("未找到该节点的在线日志");
        }

        // 设置下线时间和状态
        activeLog.setEndTime(LocalDateTime.now());
        activeLog.setStatus(NodeStatus.OFFLINE.getStatus());

        return baseMapper.updateById(activeLog) > 0;
    }

    /**
     * 格式化时长为中文（如：2小时15分、5小时42分）
     */
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        }

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        if (minutes > 0) {
            sb.append(minutes).append("分");
        }
        if (secs > 0 && hours == 0) { // 只有在小于1小时时才显示秒
            sb.append(secs).append("秒");
        }

        return sb.toString();
    }
}
