package com.xigua.center.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xigua.center.entity.PushSubscription;
import org.apache.ibatis.annotations.Mapper;

/**
 * @ClassName PushSubscriptionMapper
 * @Description Web Push 订阅 Mapper
 * @Author xigua
 * @Date 2026/11/12 19:35
 */
@Mapper
public interface PushSubscriptionMapper extends BaseMapper<PushSubscription> {
}
