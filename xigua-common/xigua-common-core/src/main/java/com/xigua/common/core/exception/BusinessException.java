package com.xigua.common.core.exception;


import java.io.Serializable;

/**
 * @ClassName BizException
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/4/27 21:15
 */
public class BusinessException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 1L;

    public BusinessException(String msg) {
        super(msg);
    }
}
