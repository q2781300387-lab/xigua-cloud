package com.xigua.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xigua.admin.domain.ConfigItem;
import com.xigua.admin.dto.ConfigPageQuery;
import com.xigua.admin.dto.ConfigSaveDTO;
import com.xigua.admin.mapper.ConfigItemMapper;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.util.BasePage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AdminConfigService {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9._-]{1,79}$");
    private static final List<String> VALUE_TYPES = List.of("string", "number", "boolean", "json");

    private final ConfigItemMapper configItemMapper;

    public AdminConfigService(ConfigItemMapper configItemMapper) {
        this.configItemMapper = configItemMapper;
    }

    public BasePageVO<ConfigItem> page(ConfigPageQuery query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);

        LambdaQueryWrapper<ConfigItem> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(ConfigItem::getConfigKey, keyword)
                    .or().like(ConfigItem::getConfigName, keyword)
                    .or().like(ConfigItem::getDescription, keyword));
        }
        if (StringUtils.isNotBlank(query.getConfigGroup())) {
            wrapper.eq(ConfigItem::getConfigGroup, query.getConfigGroup().trim());
        }
        wrapper.orderByAsc(ConfigItem::getConfigGroup).orderByAsc(ConfigItem::getId);

        Page<ConfigItem> page = configItemMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return BasePage.getResult(page, page.getRecords());
    }

    public List<String> groups() {
        QueryWrapper<ConfigItem> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT config_group").orderByAsc("config_group");
        return configItemMapper.selectObjs(wrapper).stream()
                .map(String::valueOf)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", configItemMapper.selectCount(new LambdaQueryWrapper<>()));
        result.put("editable", configItemMapper.selectCount(
                new LambdaQueryWrapper<ConfigItem>().eq(ConfigItem::getEditable, 1)));
        result.put("groups", groups().size());
        result.put("readonly", configItemMapper.selectCount(
                new LambdaQueryWrapper<ConfigItem>().eq(ConfigItem::getEditable, 0)));
        return result;
    }

    public ConfigItem create(ConfigSaveDTO dto) {
        ConfigItem item = new ConfigItem();
        apply(dto, item);
        item.setId(null);
        validate(item, true);
        configItemMapper.insert(item);
        return item;
    }

    public ConfigItem update(ConfigSaveDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("配置项ID不能为空");
        }
        ConfigItem existing = configItemMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("配置项不存在");
        }
        if (existing.getEditable() != null && existing.getEditable() == 0) {
            throw new BusinessException("该配置项为只读，不能修改");
        }

        ConfigItem item = new ConfigItem();
        apply(dto, item);
        item.setId(dto.getId());
        validate(item, false);
        item.setCreateBy(existing.getCreateBy());
        item.setCreateTime(existing.getCreateTime());
        item.setUpdateBy(null);
        item.setUpdateTime(null);
        configItemMapper.updateById(item);
        return item;
    }

    public void delete(Long id) {
        ConfigItem existing = configItemMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("配置项不存在");
        }
        if (existing.getEditable() != null && existing.getEditable() == 0) {
            throw new BusinessException("该配置项为只读，不能删除");
        }
        configItemMapper.deleteById(id);
    }

    private void apply(ConfigSaveDTO dto, ConfigItem item) {
        if (dto == null) {
            throw new BusinessException("配置内容不能为空");
        }
        item.setConfigKey(StringUtils.trimToEmpty(dto.getConfigKey()));
        item.setConfigName(StringUtils.trimToEmpty(dto.getConfigName()));
        item.setConfigValue(dto.getConfigValue() == null ? "" : dto.getConfigValue());
        item.setConfigGroup(StringUtils.defaultIfBlank(StringUtils.trim(dto.getConfigGroup()), "通用"));
        item.setValueType(StringUtils.defaultIfBlank(StringUtils.trim(dto.getValueType()), "string"));
        item.setDescription(StringUtils.trimToEmpty(dto.getDescription()));
        item.setEditable(dto.getEditable() == null ? 1 : (dto.getEditable() == 0 ? 0 : 1));
    }

    private void validate(ConfigItem item, boolean creating) {
        if (StringUtils.isBlank(item.getConfigKey())) {
            throw new BusinessException("配置键不能为空");
        }
        if (!KEY_PATTERN.matcher(item.getConfigKey()).matches()) {
            throw new BusinessException("配置键需以字母开头，仅允许字母、数字、点、下划线和短横线，长度 2-80");
        }
        if (StringUtils.isBlank(item.getConfigName())) {
            throw new BusinessException("配置名称不能为空");
        }
        if (!VALUE_TYPES.contains(item.getValueType())) {
            throw new BusinessException("值类型仅支持 string / number / boolean / json");
        }
        LambdaQueryWrapper<ConfigItem> wrapper = new LambdaQueryWrapper<ConfigItem>()
                .eq(ConfigItem::getConfigKey, item.getConfigKey());
        if (!creating && item.getId() != null) {
            wrapper.ne(ConfigItem::getId, item.getId());
        }
        if (configItemMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("配置键已存在：" + item.getConfigKey());
        }
        validateValue(item);
    }

    private void validateValue(ConfigItem item) {
        String value = item.getConfigValue();
        if (StringUtils.isBlank(value)) {
            return;
        }
        switch (item.getValueType()) {
            case "number" -> {
                try {
                    new java.math.BigDecimal(value.trim());
                } catch (NumberFormatException ex) {
                    throw new BusinessException("配置值必须是合法数字");
                }
            }
            case "boolean" -> {
                if (!"true".equalsIgnoreCase(value.trim()) && !"false".equalsIgnoreCase(value.trim())) {
                    throw new BusinessException("布尔配置值只能是 true 或 false");
                }
                item.setConfigValue(value.trim().toLowerCase());
            }
            case "json" -> {
                try {
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(value);
                } catch (Exception ex) {
                    throw new BusinessException("配置值不是合法 JSON");
                }
            }
            default -> {
            }
        }
    }
}
