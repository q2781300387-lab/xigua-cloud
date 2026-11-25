package com.xigua.center.service;

import com.xigua.api.service.OnlineUserService;
import com.xigua.center.helper.ClientNodeHelper;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.enums.RedisEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @ClassName OnlineUserServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/3/21 23:30
 */
@Service
@DubboService
public class OnlineUserServiceImpl implements OnlineUserService {
    @Autowired
    private RedisClient redisClient;

    @Override
    public Set<String> getOnlineUsers() {
        Set<String> allClientIds = ClientNodeHelper.getAllClientIps();
        if(CollectionUtils.isEmpty(allClientIds)) {
            return Set.of();
        }

        for (String clientId : allClientIds) {
            Set<Object> userIdsObj = redisClient.members(RedisEnum.ONLINE_NODE_USER.getKey() + clientId);
            if(CollectionUtils.isNotEmpty(userIdsObj)) {
                return userIdsObj.stream().map(Object::toString).collect(Collectors.toSet());
            }
        }

        return Set.of();
    }
}
