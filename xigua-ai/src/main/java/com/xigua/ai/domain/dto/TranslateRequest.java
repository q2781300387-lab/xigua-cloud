package com.xigua.ai.domain.dto;

import lombok.Data;

/**
 * 消息翻译请求
 */
@Data
public class TranslateRequest {

    /**
     * 原文
     */
    private String text;

    /**
     * 目标语言：zh / en / ja
     */
    private String target;
}
