package com.xigua.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xigua.admin.domain.OperLog;
import com.xigua.admin.dto.OperLogPageQuery;
import com.xigua.admin.mapper.OperLogMapper;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.util.BasePage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AdminOperLogService {

    private final OperLogMapper operLogMapper;

    public AdminOperLogService(OperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    public BasePageVO<OperLog> page(OperLogPageQuery query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);

        Page<OperLog> page = operLogMapper.selectPage(
                new Page<>(pageNum, pageSize), buildWrapper(query).orderByDesc("create_time"));
        return BasePage.getResult(page, page.getRecords());
    }

    public Map<String, Object> stats(OperLogPageQuery query) {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = operLogMapper.selectCount(buildWrapper(query));
        long success = operLogMapper.selectCount(buildWrapper(query).eq("status", 1));
        long failed = operLogMapper.selectCount(buildWrapper(query).eq("status", 0));

        QueryWrapper<OperLog> avgWrapper = buildWrapper(query)
                .select("IFNULL(ROUND(AVG(cost_ms), 0), 0)");
        List<Object> values = operLogMapper.selectObjs(avgWrapper);
        BigDecimal avg = values.isEmpty() || values.get(0) == null
                ? BigDecimal.ZERO : new BigDecimal(String.valueOf(values.get(0)));

        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        result.put("avgCost", avg.longValue());
        return result;
    }

    public List<String> modules() {
        QueryWrapper<OperLog> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT module").orderByAsc("module");
        return operLogMapper.selectObjs(wrapper).stream()
                .map(String::valueOf)
                .filter(StringUtils::isNotBlank)
                .collect(java.util.stream.Collectors.toList());
    }

    public void record(OperLog operLog) {
        try {
            operLogMapper.insert(operLog);
        } catch (Exception ex) {
            // 审计失败不能影响正常业务
            log.warn("写入后台操作日志失败: {}", ex.getMessage());
        }
    }

    private QueryWrapper<OperLog> buildWrapper(OperLogPageQuery query) {
        QueryWrapper<OperLog> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like("admin_name", keyword)
                    .or().like("action", keyword)
                    .or().like("request_uri", keyword)
                    .or().like("params", keyword));
        }
        if (StringUtils.isNotBlank(query.getModule())) {
            wrapper.eq("module", query.getModule().trim());
        }
        if (query.getStatus() != null && (query.getStatus() == 0 || query.getStatus() == 1)) {
            wrapper.eq("status", query.getStatus());
        }

        LocalDate start = parseDate(query.getStartTime());
        if (start != null) {
            wrapper.ge("create_time", LocalDateTime.of(start, LocalTime.MIN));
        }
        LocalDate end = parseDate(query.getEndTime());
        if (end != null) {
            wrapper.le("create_time", LocalDateTime.of(end, LocalTime.MAX));
        }
        return wrapper;
    }

    private LocalDate parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }
}
