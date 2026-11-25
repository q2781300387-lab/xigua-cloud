package com.xigua.client.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * @ClassName HomeStatMapper
 * @Description 首页「本周活跃」统计：按时间区间统计发送消息数与新增好友数
 */
@Mapper
public interface HomeStatMapper {

    /**
     * 统计区间内该用户发出的消息数
     *
     * @param userId 用户id
     * @param start  区间起点（含）
     * @param end    区间终点（不含）
     * @return 消息数
     */
    Integer countSentMessages(@Param("userId") String userId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    /**
     * 统计区间内该用户新增的好友关系数
     *
     * @param userId 用户id
     * @param start  区间起点（含）
     * @param end    区间终点（不含）
     * @return 新增好友数
     */
    Integer countNewFriends(@Param("userId") String userId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);
}
