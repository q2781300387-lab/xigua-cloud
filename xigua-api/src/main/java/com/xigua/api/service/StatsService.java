package com.xigua.api.service;

/**
 * @ClassName StatsService
 * @Description 统计服务接口
 * @Author wangjinfei
 * @Date 2026/3/10
 */
public interface StatsService {

    /**
     * 获取活跃群组数
     * @author wangjinfei
     * @date 2026/3/10
     * @return Long
     */
    Long getActiveGroups();

    /**
     * 获取好友总数
     * @author wangjinfei
     * @date 2026/3/10
     * @return Long
     */
    Long getTotalFriends();

    /**
     * 获取用户加入的群组数
     * @author wangjinfei
     * @date 2026/3/10
     * @param userId 用户ID
     * @return Long
     */
    Long getUserGroupCount(String userId);

    /**
     * 获取用户好友数
     * @author wangjinfei
     * @date 2026/3/10
     * @param userId 用户ID
     * @return Long
     */
    Long getUserFriendCount(String userId);

    /**
     * 获取用户昨日群组数
     * @author wangjinfei
     * @date 2026/3/10
     * @param userId 用户ID
     * @return Long
     */
    Long getUserYesterdayGroupCount(String userId);
}
