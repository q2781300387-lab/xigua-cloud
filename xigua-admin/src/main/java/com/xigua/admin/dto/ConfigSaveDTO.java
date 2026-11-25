package com.xigua.admin.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 全局配置新增/修改请求。
 */
@Data
public class ConfigSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String configKey;

    private String configName;

    private String configValue;

    private String configGroup;

    private String valueType;

    private String description;

    private Integer editable;
}
