package com.xigua.center.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageArchiveMapper {

    List<String> selectIdsBefore(@Param("cutoff") java.time.LocalDateTime cutoff, @Param("limit") int limit);

    int insertByIds(@Param("ids") List<String> ids);

    int deleteByIds(@Param("ids") List<String> ids);

    long countBefore(@Param("cutoff") java.time.LocalDateTime cutoff);
}
