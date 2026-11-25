package com.xigua.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.domain.bo.AiMemoryBO;
import com.xigua.domain.entity.AiMemory;

import java.util.List;

/**
 * @ClassName AiMemoryService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/2/27 21:04
 */
public interface AiMemoryService extends IService<AiMemory> {
    List<AiMemoryBO> refreshMemory(String sessionId, int size);
}
