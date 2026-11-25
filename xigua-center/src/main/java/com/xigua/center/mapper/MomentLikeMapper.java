package com.xigua.center.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xigua.domain.entity.MomentLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 朋友圈点赞表操作
 */
@Mapper
public interface MomentLikeMapper extends BaseMapper<MomentLike> {
}
