package com.xigua.domain.dto;

import com.xigua.common.mybatis.base.BasePageDTO;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName PageSessionsDTO
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/26 20:11
 */
@Data
public class PageSessionsDTO extends BasePageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String botId;
    private String userId;
}
