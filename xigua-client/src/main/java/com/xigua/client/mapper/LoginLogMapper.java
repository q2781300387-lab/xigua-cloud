package com.xigua.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xigua.domain.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * @ClassName LoginLogMapper
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/15 10:23
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
