package com.xigua.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 朋友圈用到的用户简要信息
 */
@Data
public class MomentUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String username;

    private String avatar;
}
