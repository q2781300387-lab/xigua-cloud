package com.xigua.domain.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import lombok.Data;

import java.io.Serializable;

/**
 * 通话记录分页查询参数
 */
@Data
public class GetCallRecordsDTO extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 只看未接（rejected / timeout / busy），为空表示全部
     */
    private Boolean missedOnly;

    /**
     * 对端昵称模糊搜索
     */
    private String keyword;

    /**
     * 起始时间（yyyy-MM-dd HH:mm:ss），含
     */
    private String startTime;

    /**
     * 结束时间（yyyy-MM-dd HH:mm:ss），含
     */
    private String endTime;
}
