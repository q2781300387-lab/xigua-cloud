package com.xigua.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xigua.client.entity.GroupFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GroupFileMapper extends BaseMapper<GroupFile> {

    @Select("SELECT COALESCE(SUM(file_size), 0) FROM xg_group_file WHERE uploader_id = #{userId} AND del_flag = 0")
    Long sumSizeByUploader(@Param("userId") String userId);
}
