package com.xigua.admin.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局配置分页查询。
 */
@Data
public class ConfigPageQuery extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;

    private String configGroup;
}
