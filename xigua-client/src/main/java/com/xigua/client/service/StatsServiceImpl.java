package com.xigua.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigua.api.service.GroupMemberService;
import com.xigua.api.service.StatsService;
import com.xigua.client.mapper.FriendRelationMapper;
import com.xigua.client.mapper.GroupMapper;
import com.xigua.client.mapper.GroupMemberMapper;
import com.xigua.domain.entity.FriendRelation;
import com.xigua.domain.entity.Group;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @ClassName StatsServiceImpl
 * @Description 统计服务实现
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Slf4j
@Service
@DubboService
public class StatsServiceImpl implements StatsService {

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private FriendRelationMapper friendRelationMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @DubboReference
    private GroupMemberService groupMemberService;

    @Override
    public Long getActiveGroups() {
        // 获取活跃群组数（近7天有消息的群组）
        return groupMapper.selectCount(new LambdaQueryWrapper<Group>()
                .eq(Group::getDelFlag, 0));
    }

    @Override
    public Long getTotalFriends() {
        // 好友关系表是双向存储的，需要除以2
        Long count = friendRelationMapper.selectCount(new LambdaQueryWrapper<FriendRelation>()
                .eq(FriendRelation::getDelFlag, 0));
        return (count + 1) / 2; // 向上取整避免奇数除以2后丢失
    }

    @Override
    public Long getUserGroupCount(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return 0L;
        }
        Integer count = groupMemberService.getCountByUserId(userId);
        return count != null ? count.longValue() : 0L;
    }

    @Override
    public Long getUserFriendCount(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return 0L;
        }
        Integer count = friendRelationMapper.getCountByUserId(userId);
        return count != null ? count.longValue() : 0L;
    }

    @Override
    public Long getUserYesterdayGroupCount(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return 0L;
        }
        // 计算昨日结束时间
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime yesterdayEnd = LocalDateTime.of(yesterday, LocalTime.MAX);
        String yesterdayEndStr = yesterdayEnd.toString().replace("T", " ");

        Integer count = groupMemberMapper.getCountByUserIdBeforeTime(userId, yesterdayEndStr);
        return count != null ? count.longValue() : 0L;
    }
}
