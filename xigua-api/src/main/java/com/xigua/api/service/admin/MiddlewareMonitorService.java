package com.xigua.api.service.admin;

import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.dto.MiddlewareQueryDTO;
import com.xigua.domain.vo.admin.MiddlewareStatsVO;
import com.xigua.domain.vo.admin.MiddlewarePageVO;

/**
 * @ClassName IMiddlewareMonitorService
 * @Description 中间件监控服务接口
 * @Author Claude Code
 * @Date 2026/5/8
 */
public interface MiddlewareMonitorService {

    /**
     * 获取中间件统计卡片数据
     *
     * @return 中间件统计数据
     */
    MiddlewareStatsVO getMiddlewareStats();

    /**
     * 分页查询中间件列表
     *
     * @param queryDTO 查询条件
     * @return 分页数据
     */
    BasePageVO<MiddlewarePageVO> getMiddlewarePage(MiddlewareQueryDTO queryDTO);
}
