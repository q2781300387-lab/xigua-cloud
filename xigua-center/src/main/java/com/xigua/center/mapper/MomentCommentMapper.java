package com.xigua.center.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xigua.domain.entity.MomentComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 朋友圈评论表操作
 */
@Mapper
public interface MomentCommentMapper extends BaseMapper<MomentComment> {
}
