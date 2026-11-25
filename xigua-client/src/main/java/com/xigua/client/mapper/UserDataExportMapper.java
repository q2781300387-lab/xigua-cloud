package com.xigua.client.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserDataExportMapper {

    @Select("SELECT id, client_msg_id, sender_id, receiver_id, message, chat_type, message_type, create_time "
            + "FROM xg_chat_message WHERE del_flag = 0 AND (sender_id = #{userId} OR receiver_id = #{userId}) "
            + "ORDER BY create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> selectMessages(@Param("userId") String userId, @Param("limit") int limit);
}
