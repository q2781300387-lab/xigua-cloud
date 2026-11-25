package com.xigua.ai.llm.model;

import lombok.Data;

/**
 * @ClassName Memory
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/27 22:02
 */
@Data
public class Memory {
    private String role;
    private String content;
}
