package com.xigua.api.service.admin;

import com.xigua.domain.vo.admin.ActiveGroupRankVO;
import com.xigua.domain.vo.admin.AdminHomeStatisticsVO;
import com.xigua.domain.vo.admin.MessageTrendVO;

/**
 * @ClassName AdminStatisticsService
 * @Description 管理端统计服务
 * @Author wangjinfei
 * @Date 2026/3/9
 */
public interface AdminStatisticsService {

    /**
     * 获取首页统计信息
     * @author wangjinfei
     * @date 2026/3/9
     * @return AdminHomeStatisticsVO
    */
    AdminHomeStatisticsVO getHomeStatistics();

    /**
     * 获取消息发送趋势
     * @author wangjinfei
     * @date 2026/3/9
     * @return MessageTrendVO
    */
    MessageTrendVO getMessageTrend();

    /**
     * 获取活跃群组排行
     * @author wangjinfei
     * @date 2026/3/9
     * @return ActiveGroupRankVO
    */
    ActiveGroupRankVO getActiveGroupRank();
}
