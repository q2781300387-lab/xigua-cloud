package com.xigua.admin.service;

import com.xigua.api.service.ChatMessageService;
import com.xigua.api.service.GroupService;
import com.xigua.api.service.UserService;
import com.xigua.api.service.admin.AdminStatisticsService;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.vo.admin.ActiveGroupRankItemVO;
import com.xigua.domain.vo.admin.ActiveGroupRankVO;
import com.xigua.domain.vo.admin.AdminHomeStatisticsVO;
import com.xigua.domain.vo.admin.MessageTrendItemVO;
import com.xigua.domain.vo.admin.MessageTrendVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @ClassName AdminStatisticsServiceImpl
 * @Description 管理端统计服务实现
 * @Author wangjinfei
 * @Date 2026/3/9
 */
@Slf4j
@Service
@DubboService
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    @Autowired
    private RedisClient redisClient;

    @DubboReference
    private UserService userService;

    @DubboReference
    private ChatMessageService chatMessageService;

    @DubboReference
    private GroupService groupService;

    private static final String STATISTICS_CACHE_KEY = "admin:home:statistics";
    private static final String TREND_CACHE_KEY = "admin:home:message:trend";
    private static final String GROUP_RANK_CACHE_KEY = "admin:home:group:rank";
    private static final int CACHE_TTL_SECONDS = 300; // 5分钟缓存

    @Override
    public AdminHomeStatisticsVO getHomeStatistics() {
        // 尝试从缓存获取
        String cachedData = redisClient.get(STATISTICS_CACHE_KEY);
        if (StringUtils.isNotEmpty(cachedData)) {
            try {
                AdminHomeStatisticsVO cachedVO = com.alibaba.fastjson2.JSONObject.parseObject(
                    cachedData, AdminHomeStatisticsVO.class
                );
                if (cachedVO != null) {
                    return cachedVO;
                }
            } catch (Exception e) {
                log.warn("Failed to parse cached statistics", e);
            }
        }

        // 缓存失效，重新计算
        AdminHomeStatisticsVO result = calculateHomeStatistics();

        // 存入缓存
        try {
            redisClient.set(STATISTICS_CACHE_KEY,
                com.alibaba.fastjson2.JSONObject.toJSONString(result), CACHE_TTL_SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache statistics", e);
        }

        return result;
    }

    /**
     * 计算首页统计数据
     */
    private AdminHomeStatisticsVO calculateHomeStatistics() {
        AdminHomeStatisticsVO vo = new AdminHomeStatisticsVO();

        // 获取总注册用户数及增长率
        Long totalUserCount = userService.countTotalUsers();
        Long yesterdayUserCount = userService.countUsersByTime(getYesterdayStartMillis());
        vo.setTotalUserCount(totalUserCount);
        vo.setTotalUserGrowthRate(calculateGrowthRate(totalUserCount, yesterdayUserCount));

        // 获取实时在线用户数
        Long onlineUserCount = getOnlineUserCount();
        vo.setOnlineUserCount(onlineUserCount);
        // 在线用户增长率暂设为0（无历史数据）
        vo.setOnlineUserGrowthRate(0.0);

        // 获取今日和昨日消息总量
        Long todayMessageCount = chatMessageService.countTodayMessages();
        Long yesterdayMessageCount = chatMessageService.countYesterdayMessages();
        vo.setTodayMessageCount(todayMessageCount);
        vo.setTodayMessageGrowthRate(calculateGrowthRate(todayMessageCount, yesterdayMessageCount));

        // 获取活跃群组数及增长率
        Long activeGroupCount = groupService.countActiveGroups();
        Long yesterdayActiveGroupCount = groupService.countYesterdayActiveGroups();
        vo.setActiveGroupCount(activeGroupCount);
        vo.setActiveGroupGrowthRate(calculateGrowthRate(activeGroupCount, yesterdayActiveGroupCount));

        return vo;
    }

    /**
     * 计算增长百分比
     * 公式：(current - previous) / previous * 100
     */
    private Double calculateGrowthRate(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return (double) (current - previous) / previous * 100;
    }

    /**
     * 获取实时在线用户数
     * 遍历 Redis 中所有 online_node_user:* 的 Set，去重统计
     */
    private Long getOnlineUserCount() {
        try {
            Set<String> keys = redisClient.getKeysByKey(RedisEnum.ONLINE_NODE_USER.getKey() + "*");
            Set<String> allOnlineUsers = new HashSet<>();

            if (!CollectionUtils.isEmpty(keys)) {
                for (String key : keys) {
                    Set<Object> nodeUsers = redisClient.members(key);
                    if (!CollectionUtils.isEmpty(nodeUsers)) {
                        for (Object user : nodeUsers) {
                            allOnlineUsers.add(user.toString());
                        }
                    }
                }
            }

            return (long) allOnlineUsers.size();
        } catch (Exception e) {
            log.warn("Failed to get online user count", e);
            return 0L;
        }
    }

    /**
     * 获取昨天0点的时间戳（毫秒）
     */
    private Long getYesterdayStartMillis() {
        LocalDateTime yesterdayStart = LocalDateTime.of(
            LocalDate.now().minusDays(1), LocalTime.MIN
        );
        return yesterdayStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Override
    public MessageTrendVO getMessageTrend() {
        // 尝试从缓存获取
        String cachedData = redisClient.get(TREND_CACHE_KEY);
        if (StringUtils.isNotEmpty(cachedData)) {
            try {
                MessageTrendVO cachedVO = com.alibaba.fastjson2.JSONObject.parseObject(
                    cachedData, MessageTrendVO.class
                );
                if (cachedVO != null) {
                    return cachedVO;
                }
            } catch (Exception e) {
                log.warn("Failed to parse cached message trend", e);
            }
        }

        // 缓存失效，重新计算
        MessageTrendVO result = calculateMessageTrend();

        // 存入缓存
        try {
            redisClient.set(TREND_CACHE_KEY,
                com.alibaba.fastjson2.JSONObject.toJSONString(result), CACHE_TTL_SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache message trend", e);
        }

        return result;
    }

    /**
     * 计算消息趋势数据
     */
    private MessageTrendVO calculateMessageTrend() {
        MessageTrendVO vo = new MessageTrendVO();

        // 获取24小时趋势数据
        List<MessageTrendItemVO> trendData = chatMessageService.getMessageTrend24Hours();

        // 设置时间范围描述
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        String endTime = now.format(formatter);
        String startTime = now.minusHours(23).format(formatter);
        vo.setPeriod(startTime + " ~ " + endTime);

        vo.setTrendData(trendData);

        return vo;
    }

    @Override
    public ActiveGroupRankVO getActiveGroupRank() {
        // 尝试从缓存获取
        String cachedData = redisClient.get(GROUP_RANK_CACHE_KEY);
        if (StringUtils.isNotEmpty(cachedData)) {
            try {
                ActiveGroupRankVO cachedVO = com.alibaba.fastjson2.JSONObject.parseObject(
                    cachedData, ActiveGroupRankVO.class
                );
                if (cachedVO != null) {
                    return cachedVO;
                }
            } catch (Exception e) {
                log.warn("Failed to parse cached group rank", e);
            }
        }

        // 缓存失效，重新计算
        ActiveGroupRankVO result = calculateActiveGroupRank();

        // 存入缓存
        try {
            redisClient.set(GROUP_RANK_CACHE_KEY,
                com.alibaba.fastjson2.JSONObject.toJSONString(result), CACHE_TTL_SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache group rank", e);
        }

        return result;
    }

    /**
     * 计算活跃群组排行数据
     */
    private ActiveGroupRankVO calculateActiveGroupRank() {
        ActiveGroupRankVO vo = new ActiveGroupRankVO();

        // 获取排行数据
        List<ActiveGroupRankItemVO> rankList = groupService.getActiveGroupRank();

        vo.setTitle("活跃群组排行");
        vo.setRankList(rankList);

        return vo;
    }
}
