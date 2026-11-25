package com.xigua.center.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xigua.domain.entity.TimedMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * @ClassName TimedMessageMapper
 * @Description 定时消息表操作
 */
@Mapper
public interface TimedMessageMapper extends BaseMapper<TimedMessage> {
}
