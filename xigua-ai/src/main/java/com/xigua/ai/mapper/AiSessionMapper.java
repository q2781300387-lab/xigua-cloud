package com.xigua.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xigua.domain.dto.PageSessionsDTO;
import com.xigua.domain.entity.AiSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @ClassName AiSessionMapper
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/2/24 21:14
 */
@Mapper
public interface AiSessionMapper extends BaseMapper<AiSession> {
    List<AiSession> pageSessions(Page<AiSession> page, @Param("query") PageSessionsDTO query);
}
