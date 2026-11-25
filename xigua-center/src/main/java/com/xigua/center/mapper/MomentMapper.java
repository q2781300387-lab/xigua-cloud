package com.xigua.center.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xigua.domain.entity.Moment;
import com.xigua.domain.vo.MomentUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 朋友圈动态表操作
 */
@Mapper
public interface MomentMapper extends BaseMapper<Moment> {

    /**
     * 查发布人/点赞人/评论人的昵称与头像
     */
    @Select("SELECT id, username, avatar FROM xg_user WHERE id = #{userId} AND del_flag = 0")
    MomentUserVO selectUserBrief(@Param("userId") String userId);

    /**
     * 查当前用户的好友 id（朋友圈可见范围）
     */
    @Select("SELECT friend_id FROM xg_friend_relation WHERE user_id = #{userId} AND del_flag = 0")
    List<String> selectFriendIds(@Param("userId") String userId);
}
