package com.xigua.api.service;

import com.xigua.domain.bo.NodeSystemInfo;

/**
 * @ClassName NodeSystemService
 * @Description 节点系统信息服务接口
 * @Author wangjinfei
 * @Date 2026/3/18
 */
public interface NodeClientService {

    /**
     * 手动刷新节点系统信息
     * @author wangjinfei
     * @date 2026/3/18
     * @return Boolean 刷新是否成功
     */
    NodeSystemInfo refreshSystemInfo();
}
