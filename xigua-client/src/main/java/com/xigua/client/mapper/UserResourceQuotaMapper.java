package com.xigua.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xigua.domain.entity.UserResourceQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @ClassName UserResourceQuotaMapper
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/27 11:16
 */
@Mapper
public interface UserResourceQuotaMapper extends BaseMapper<UserResourceQuota> {
    UserResourceQuota getByUserId(@Param("userId") String userId);
}
