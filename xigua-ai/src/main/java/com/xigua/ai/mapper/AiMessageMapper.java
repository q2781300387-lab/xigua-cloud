package com.xigua.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xigua.domain.dto.GetAiHistoryMes;
import com.xigua.domain.entity.AiMessage;
import com.xigua.domain.vo.AiHistoryMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @ClassName AiMessageMapper
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/2/24 21:15
 */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
    List<AiMessage> lastMessages(@Param("sessionId") String sessionId, @Param("size") int size);
    List<AiHistoryMessageVO> pageMessages(Page<AiHistoryMessageVO> page, @Param("query") GetAiHistoryMes dto);
}
