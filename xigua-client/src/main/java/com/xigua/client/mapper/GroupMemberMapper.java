package com.xigua.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xigua.domain.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * @ClassName GroupMemberMapper
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/7/6 11:15
 */
@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    /**
     * 根据用户ID查询用户加入的群组数量
     * @author wangjinfei
     * @date 2025/7/27 9:44
     * @param userId
     * @return Integer
     */
    Integer getCountByUserId(String userId);

    /**
     * 根据用户ID查询截止昨日加入的群组数量
     * @author wangjinfei
     * @date 2026/3/10
     * @param userId 用户ID
     * @param yesterdayEnd 昨日结束时间
     * @return Integer
     */
    Integer getCountByUserIdBeforeTime(String userId, String yesterdayEnd);
}
