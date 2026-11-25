package com.xigua.client.service;

import com.xigua.api.service.FriendRelationService;
import com.xigua.api.service.GroupMemberService;
import com.xigua.api.service.HomeService;
import com.xigua.client.mapper.HomeStatMapper;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.vo.HomeCountVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * @ClassName HomeServiceImpl
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/8/9 10:54
 */
@Slf4j
@Service
@DubboService
public class HomeServiceImpl implements HomeService {
    @Autowired
    private FriendRelationService friendRelationService;
    @Autowired
    private GroupMemberService groupMemberService;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private HomeStatMapper homeStatMapper;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 获取首页统计信息
     *
     * @author wangjinfei
     * @date 2025/8/9 10:56
     * @return HomeCountVO
     */
    @Override
    public HomeCountVO getHomeCount() {
        String userId = UserContext.get().getUserId();
        HomeCountVO homeCountVO = new HomeCountVO();

        // 好友数量
        homeCountVO.setFriendCount(friendRelationService.getCountByUserId(userId));
        // 群数量
        homeCountVO.setGroupCount(groupMemberService.getCountByUserId(userId));

        // 未读消息
        // 好友未读消息
        Long unreadCount = redisClient.hincrget(RedisEnum.FRIEND_UNREAD_COUNT.getKey() + userId);
        homeCountVO.setUnreadCount(unreadCount);

        fillWeekActivity(homeCountVO, userId);

        return homeCountVO;
    }

    /**
     * 填充「本周活跃」：本周实际数据 + 与上周同期的环比。
     * <p>
     * 周期口径：周一到周日。上周取“同期”，即同样只统计已经过完的那几天，
     * 否则拿本周 3 天去比上周整 7 天，趋势永远显示下跌。
     * </p>
     */
    private void fillWeekActivity(HomeCountVO vo, String userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);
        int elapsedDays = (int) (ChronoUnit.DAYS.between(weekStartDate, today) + 1);

        // 本周：[周一 00:00, 明天 00:00)
        LocalDateTime curStart = weekStartDate.atStartOfDay();
        LocalDateTime curEnd = today.plusDays(1).atStartOfDay();
        // 上周同期：[上周一 00:00, 上周一 + 已过天数 00:00)
        LocalDateTime prevStart = weekStartDate.minusDays(7).atStartOfDay();
        LocalDateTime prevEnd = weekStartDate.minusDays(7).plusDays(elapsedDays).atStartOfDay();

        int curMsg = safeCount(homeStatMapper.countSentMessages(userId, curStart, curEnd));
        int prevMsg = safeCount(homeStatMapper.countSentMessages(userId, prevStart, prevEnd));
        int curFriend = safeCount(homeStatMapper.countNewFriends(userId, curStart, curEnd));
        int prevFriend = safeCount(homeStatMapper.countNewFriends(userId, prevStart, prevEnd));

        vo.setWeekStart(weekStartDate.format(DAY_FMT));
        vo.setWeekEnd(weekStartDate.plusDays(6).format(DAY_FMT));
        vo.setElapsedDays(elapsedDays);
        vo.setWeekMessageCount(curMsg);
        vo.setWeekFriendCount(curFriend);
        vo.setWeekDailyAvg(round1(curMsg / (double) elapsedDays));

        vo.setMessageTrend(trend(curMsg, prevMsg));
        vo.setFriendTrend(trend(curFriend, prevFriend));
        vo.setDailyTrend(trend(curMsg / (double) elapsedDays, prevMsg / (double) elapsedDays));
    }

    /**
     * 环比百分比，保留一位小数。上周同期为 0 时没有可比的基准，返回 null。
     */
    private Double trend(double cur, double prev) {
        if (prev <= 0) {
            return null;
        }
        return round1((cur - prev) / prev * 100.0);
    }

    private Double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private int safeCount(Integer v) {
        return v == null ? 0 : v;
    }
}
