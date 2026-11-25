package com.xigua.center.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.api.service.NodeClientService;
import com.xigua.api.service.UserService;
import com.xigua.api.service.admin.NodeLogService;
import com.xigua.api.service.admin.NodeService;
import com.xigua.center.mapper.NodeMapper;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.IpUtil;
import com.xigua.common.core.util.UserAgentUtil;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.bo.NodeSystemInfo;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.common.mybatis.base.PagePlus;
import com.xigua.domain.dto.QueryNodeOnlineUserDTO;
import com.xigua.domain.dto.QueryNodePageDTO;
import com.xigua.domain.dto.SaveNodeDTO;
import com.xigua.domain.entity.Node;
import com.xigua.domain.entity.NodeLog;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.NodeStatus;
import com.xigua.domain.util.BasePage;
import com.xigua.domain.vo.admin.NodeDetailVO;
import com.xigua.domain.vo.admin.NodeMetricsVO;
import com.xigua.domain.vo.admin.NodeOnlineUserVO;
import com.xigua.domain.vo.admin.NodeStatisticsVO;
import com.xigua.domain.vo.admin.NodeVO;
import org.apache.commons.lang3.StringUtils;
import com.xigua.common.dubbo.util.DubboUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @ClassName NodeServiceImpl
 * @Description 节点服务实现
 * @Author wangjinfei
 * @Date 2026/2/1 13:41
 */
@Slf4j
@Service
@DubboService
public class NodeServiceImpl extends ServiceImpl<NodeMapper, Node> implements NodeService {

    @Autowired
    private RedisClient redisClient;
    @Lazy
    @Autowired
    private NodeLogService nodeLogService;
    @DubboReference
    private NodeClientService nodeClientService;
    @DubboReference
    private UserService userService;

    @Override
    public BasePageVO<NodeVO> pageNodes(QueryNodePageDTO dto) {
        Page<Node> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        LambdaQueryWrapper<Node> wrapper = new LambdaQueryWrapper<>();
        // 节点名称模糊查询
        if (StringUtils.isNotEmpty(dto.getNodeName())) {
            wrapper.like(Node::getNodeName, dto.getNodeName());
        }
        // 节点编码精确查询
        if (StringUtils.isNotEmpty(dto.getNodeCode())) {
            wrapper.eq(Node::getNodeCode, dto.getNodeCode());
        }
        // 地区模糊查询
        if (StringUtils.isNotEmpty(dto.getRegion())) {
            wrapper.like(Node::getRegion, dto.getRegion());
        }
        // 按创建时间倒序
        wrapper.orderByDesc(Node::getCreateTime);

        Page<Node> nodePage = baseMapper.selectPage(page, wrapper);

        List<NodeVO> voList = nodePage.getRecords().stream().map(node -> {
            NodeVO vo = new NodeVO();
            BeanUtils.copyProperties(node, vo);
            // 从Redis获取节点系统信息
            String redisKey = RedisEnum.CLIENT_NODE_SYSTEM.getKey() + node.getInnerIp();
            NodeSystemInfo systemInfo = redisClient.get(redisKey, NodeSystemInfo.class);

            // 设置在线状态（根据Redis中是否有系统信息判断）
            if (systemInfo != null) {
                vo.setStatus(1); // 在线
                vo.setStatusDesc("在线");
            } else {
                vo.setStatus(0); // 离线
                vo.setStatusDesc("离线");
            }

            // 设置运行时信息
            // 从Redis获取节点连接数
            if (StringUtils.isNotEmpty(node.getInnerIp())) {
                String userSetKey = RedisEnum.ONLINE_NODE_USER.getKey() + node.getInnerIp();
                Long connectionCount = redisClient.scard(userSetKey);
                vo.setConnectionCount(connectionCount != null ? connectionCount.intValue() : 0);
            } else {
                vo.setConnectionCount(0);
            }
            vo.setCpuUsage(systemInfo != null ? Math.round(systemInfo.getCpuUsage() * 100.0) / 100.0 : 0.0);
            vo.setMemoryUsage(systemInfo != null ? Math.round(systemInfo.getMemoryUsage() * 100.0) / 100.0 : 0.0);

            // 获取网络延迟（从节点上报的系统信息中读取）
            vo.setLatency(systemInfo != null && systemInfo.getNetworkLatency() != null
                    ? systemInfo.getNetworkLatency()
                    : -1L);

            return vo;
        }).collect(Collectors.toList());

        PagePlus<Node> pagePlus = PagePlus.fromPage(nodePage);
        return BasePage.getResult(pagePlus, voList);
    }

    @Override
    public NodeVO getNodeById(String id) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }

        Node node = baseMapper.selectById(id);
        if (node == null) {
            return null;
        }

        NodeVO vo = new NodeVO();
        BeanUtils.copyProperties(node, vo);
        return vo;
    }

    @Override
    public Boolean saveNode(SaveNodeDTO dto) {
        if (dto == null) {
            throw new BusinessException("节点信息不能为空");
        }

        Node node = new Node();
        BeanUtils.copyProperties(dto, node);

        // 更新操作
        if (StringUtils.isNotEmpty(dto.getId())) {
            Node existingNode = baseMapper.selectById(dto.getId());
            if (existingNode == null) {
                throw new BusinessException("节点不存在");
            }
            return baseMapper.updateById(node) > 0;
        }

        // 新增操作
        return baseMapper.insert(node) > 0;
    }

    @Override
    public Boolean deleteNodes(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("节点ID列表不能为空");
        }

        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public void refreshNodeSystem(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new BusinessException("节点ID不能为空");
        }

        Node node = baseMapper.selectById(id);
        if (node == null) {
            throw new BusinessException("节点不存在");
        }

        if (StringUtils.isEmpty(node.getInnerIp())) {
            throw new BusinessException("节点内网IP为空");
        }

        // 检查节点实例是否在线（通过Redis判断）
        String redisKey = RedisEnum.CLIENT_NODE_SYSTEM.getKey() + node.getInnerIp();
        NodeSystemInfo systemInfo = redisClient.get(redisKey, NodeSystemInfo.class);
        if (systemInfo == null) {
            throw new BusinessException("节点实例不在线或未注册");
        }

        // 调用指定节点实例方法 刷新系统信息
        DubboUtil.invoke(NodeClientService.class, node.getInnerIp(), 20884,
                NodeClientService::refreshSystemInfo);
    }

    @Override
    public NodeStatisticsVO getNodeStatistics() {
        NodeStatisticsVO vo = new NodeStatisticsVO();

        // 1. 总节点数
        long totalNodes = baseMapper.selectCount(new LambdaQueryWrapper<>());
        vo.setTotalNodes(totalNodes);

        // 2. 查询所有节点
        List<Node> allNodes = baseMapper.selectList(new LambdaQueryWrapper<>());

        // 3. 统计在线节点数和实时总连接数
        long onlineNodes = 0;
        long totalConnections = 0;

        for (Node node : allNodes) {
            if (StringUtils.isEmpty(node.getInnerIp())) {
                continue;
            }

            // 检查节点是否在线（通过Redis中的系统信息判断）
            String redisKey = RedisEnum.CLIENT_NODE_SYSTEM.getKey() + node.getInnerIp();
            NodeSystemInfo systemInfo = redisClient.get(redisKey, NodeSystemInfo.class);

            if (systemInfo != null) {
                onlineNodes++;

                // 统计该节点的连接数（从Redis的ONLINE_NODE_USER set中获取）
                String userSetKey = RedisEnum.ONLINE_NODE_USER.getKey() + node.getInnerIp();
                Long connectionCount = redisClient.scard(userSetKey);
                if (connectionCount != null) {
                    totalConnections += connectionCount;
                }
            }
        }

        vo.setOnlineNodes(onlineNodes);
        vo.setTotalConnections(totalConnections);
        vo.setOfflineNodes(totalNodes - onlineNodes);

        return vo;
    }

    @Override
    public Node getByInnerIp(String innerIp) {
        if (StringUtils.isEmpty(innerIp)) {
            return null;
        }

        return baseMapper.selectOne(
                new LambdaQueryWrapper<Node>().eq(Node::getInnerIp, innerIp)
        );
    }

    @Override
    public NodeDetailVO getNodeDetail(String id) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }

        // 1. 查询节点基本信息
        Node node = baseMapper.selectById(id);
        if (node == null) {
            return null;
        }

        NodeDetailVO vo = new NodeDetailVO();
        BeanUtils.copyProperties(node, vo);

        // 2. 从Redis获取节点系统信息
        String redisKey = RedisEnum.CLIENT_NODE_SYSTEM.getKey() + node.getInnerIp();
        NodeSystemInfo systemInfo = redisClient.get(redisKey, NodeSystemInfo.class);

        // 3. 设置在线状态
        if (systemInfo != null) {
            vo.setStatus(NodeStatus.ONLINE.getStatus());
            vo.setStatusDesc(NodeStatus.ONLINE.getDesc());
        } else {
            vo.setStatus(NodeStatus.OFFLINE.getStatus());
            vo.setStatusDesc(NodeStatus.OFFLINE.getDesc());
        }

        // 4. 设置运行时信息
        if (StringUtils.isNotEmpty(node.getInnerIp())) {
            String userSetKey = RedisEnum.ONLINE_NODE_USER.getKey() + node.getInnerIp();
            Long connectionCount = redisClient.scard(userSetKey);
            vo.setConnectionCount(connectionCount != null ? connectionCount.intValue() : 0);
        } else {
            vo.setConnectionCount(0);
        }
        vo.setCpuUsage(systemInfo != null ? Math.round(systemInfo.getCpuUsage() * 100.0) / 100.0 : 0.0);
        vo.setMemoryUsage(systemInfo != null ? Math.round(systemInfo.getMemoryUsage() * 100.0) / 100.0 : 0.0);
        vo.setTps(0); // 消息吞吐量暂时返回0

        // 5. 查询最新的在线日志获取启动时间
        NodeLog latestLog = nodeLogService.getOne(
                new LambdaQueryWrapper<NodeLog>()
                        .eq(NodeLog::getNodeId, id)
                        .eq(NodeLog::getStatus, NodeStatus.ONLINE.getStatus())
                        .orderByDesc(NodeLog::getStartTime)
                        .last("LIMIT 1")
        );
        if (latestLog != null) {
            vo.setStartTime(latestLog.getStartTime());
        }

        return vo;
    }

    @Override
    public NodeMetricsVO getNodeMetrics(String id) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }

        // 1. 查询节点基本信息
        Node node = baseMapper.selectById(id);
        if (node == null) {
            return null;
        }

        NodeMetricsVO vo = new NodeMetricsVO();

        // 2. 从Redis获取节点系统信息
        String redisKey = RedisEnum.CLIENT_NODE_SYSTEM.getKey() + node.getInnerIp();
        NodeSystemInfo systemInfo = redisClient.get(redisKey, NodeSystemInfo.class);

        // 3. 设置连接人数
        if (StringUtils.isNotEmpty(node.getInnerIp())) {
            String userSetKey = RedisEnum.ONLINE_NODE_USER.getKey() + node.getInnerIp();
            Long connectionCount = redisClient.scard(userSetKey);
            vo.setConnectionCount(connectionCount != null ? connectionCount.intValue() : 0);
        } else {
            vo.setConnectionCount(0);
        }

        // 4. 设置CPU和内存使用率
        vo.setCpuUsage(systemInfo != null ? Math.round(systemInfo.getCpuUsage() * 100.0) / 100.0 : 0.0);
        vo.setMemoryUsage(systemInfo != null ? Math.round(systemInfo.getMemoryUsage() * 100.0) / 100.0 : 0.0);

        // 5. 设置消息吞吐量
        vo.setTps(0); // 暂时返回0

        return vo;
    }

    @Override
    public BasePageVO<NodeOnlineUserVO> pageNodeOnlineUsers(QueryNodeOnlineUserDTO dto) {
        if (dto == null || StringUtils.isEmpty(dto.getNodeId())) {
            throw new BusinessException("节点ID不能为空");
        }

        // 1. 查询节点获取内网IP
        Node node = baseMapper.selectById(dto.getNodeId());
        if (node == null) {
            throw new BusinessException("节点不存在");
        }

        if (StringUtils.isEmpty(node.getInnerIp())) {
            throw new BusinessException("节点内网IP为空");
        }

        // 2. 从Redis获取该节点的所有在线用户ID
        String userSetKey = RedisEnum.ONLINE_NODE_USER.getKey() + node.getInnerIp();
        Set<Object> userIdsObj = redisClient.members(userSetKey);

        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        Page<NodeOnlineUserVO> page = new Page<>(pageNum, pageSize);
        if (userIdsObj == null || userIdsObj.isEmpty()) {
            // 没有在线用户，返回空列表
            return BasePage.emptyResult(page);
        }

        // 3. 转换为List（Object -> String）
        List<String> userIdList = userIdsObj.stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        // 4. 查询用户详细信息
        List<User> users = userService.listByIds(userIdList);

        // 5. 如果有搜索关键字，进行过滤
        if (StringUtils.isNotEmpty(dto.getKeyword())) {
            users = users.stream()
                    .filter(user -> user.getId().contains(dto.getKeyword())
                            || (user.getUsername() != null && user.getUsername().contains(dto.getKeyword())))
                    .collect(Collectors.toList());
        }

        // 6. 构建VO列表（包含在线详情信息）
        List<NodeOnlineUserVO> voList = new ArrayList<>();
        for (User user : users) {
            String onlineUserKey = RedisEnum.ONLINE_USER.getKey() + user.getId();
            String onlineUserData = redisClient.get(onlineUserKey);
            if (StringUtils.isEmpty(onlineUserData)) {
                continue;
            }

            try {
                com.alibaba.fastjson2.JSONObject onlineJson = com.alibaba.fastjson2.JSONObject.parseObject(onlineUserData);
                NodeOnlineUserVO vo = new NodeOnlineUserVO();
                vo.setUserId(user.getId());
                vo.setUsername(user.getUsername());
                vo.setAvatar(user.getAvatar());

                // 从在线信息中解析 ua、loginIp、loginTime
                String ua = onlineJson.getString("ua");
                String loginIp = onlineJson.getString("loginIp");
                Long loginTime = onlineJson.getLong("loginTime");

                // 使用 UserAgentUtil 解析设备信息
                vo.setDevice(UserAgentUtil.getPlatform(ua));

                // 设置客户端IP
                vo.setClientIp(loginIp != null ? IpUtil.maskIp(loginIp) : "");

                // 计算连接时长
                if (loginTime != null) {
                    long duration = (System.currentTimeMillis() - loginTime) / 1000; // 秒
                    vo.setDuration(duration);
                    vo.setDurationFormat(formatDuration(duration));
                } else {
                    vo.setDuration(0L);
                    vo.setDurationFormat("00:00:00");
                }

                // 从Redis获取最后心跳时间
                String lastPingKey = RedisEnum.LAST_PING_TIME.getKey() + user.getId();
                String lastPingTimeStr = redisClient.get(lastPingKey);
                if (StringUtils.isNotEmpty(lastPingTimeStr)) {
                    try {
                        long lastPingTimestamp = Long.parseLong(lastPingTimeStr);
                        vo.setLastHeartbeat(java.time.LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(lastPingTimestamp),
                                java.time.ZoneId.systemDefault()
                        ));
                    } catch (NumberFormatException e) {
                        vo.setLastHeartbeat(null);
                    }
                } else {
                    vo.setLastHeartbeat(null);
                }

                voList.add(vo);
            } catch (Exception e) {
                // 解析失败，跳过该用户
                continue;
            }
        }

        // 7. 手动分页
        int total = voList.size();
        int fromIndex = (dto.getPageNum() - 1) * dto.getPageSize();
        int toIndex = Math.min(fromIndex + dto.getPageSize(), total);

        List<NodeOnlineUserVO> pageList = voList.subList(fromIndex, toIndex);

        // 8. 构建返回结果
        BasePageVO<NodeOnlineUserVO> result = new BasePageVO<>();
        result.setPageNum(dto.getPageNum());
        result.setPageSize(dto.getPageSize());
        result.setRows(pageList);
        result.setTotalCount((long) total);
        result.setTotalPage(total == 0 ? 0L : (long) ((total + dto.getPageSize() - 1) / dto.getPageSize()));

        return result;
    }

    @Override
    public Boolean online(String nodeIp) {
        if (StringUtils.isEmpty(nodeIp)) {
            throw new BusinessException("节点IP不能为空");
        }

        Node node = getByInnerIp(nodeIp);
        if (node == null) {
            throw new BusinessException("节点不存在");
        }

        // 检查节点实例是否在线（通过Redis判断）
        String redisKey = RedisEnum.CLIENT_NODE_SYSTEM.getKey() + node.getInnerIp();
        NodeSystemInfo systemInfo = redisClient.get(redisKey, NodeSystemInfo.class);
        if (systemInfo == null) {
            log.info("节点[{}]实例不在线，无法上线", node.getNodeName());
            return false;
        }

        nodeLogService.recordNodeOnline(node);
        return true;
    }

    @Override
    public Boolean offline(String nodeIp) {
        if (StringUtils.isEmpty(nodeIp)) {
            throw new BusinessException("节点IP不能为空");
        }

        Node node = getByInnerIp(nodeIp);
        if (node == null) {
            throw new BusinessException("节点不存在");
        }

        // 删除Redis中的节点系统信息
        if (StringUtils.isNotEmpty(node.getInnerIp())) {
            String redisKey = RedisEnum.CLIENT_NODE_SYSTEM.getKey() + node.getInnerIp();
            redisClient.del(redisKey);
        }

        nodeLogService.recordNodeOffline(node);
        return true;
    }

    /**
     * 格式化时长为 HH:mm:ss
     */
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
