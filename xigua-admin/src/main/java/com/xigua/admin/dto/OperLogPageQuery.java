package com.xigua.admin.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import lombok.Data;

import java.io.Serializable;

/**
 * 操作日志分页查询。
 */
@Data
public class OperLogPageQuery extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;

    private String module;

    private Integer status;

    private String startTime;

    private String endTime;
}
