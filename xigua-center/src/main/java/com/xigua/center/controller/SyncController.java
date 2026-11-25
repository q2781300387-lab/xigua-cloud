package com.xigua.center.controller;

import com.xigua.api.service.SearchSyncService;
import com.xigua.api.service.SyncRegionService;
import com.xigua.api.service.SyncUserResourceQuotaService;
import com.xigua.common.core.base.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName SyncController
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/27 19:41
 */
@Tag(name = "同步接口")
@RequestMapping("/sync")
@RestController
public class SyncController {
    @Autowired
    private SyncUserResourceQuotaService SyncUserResourceQuotaService;
    @DubboReference
    private SyncRegionService syncRegionService;
    @DubboReference
    private SearchSyncService searchSyncService;

    @Operation(summary = "同步用户资源容量")
    @PostMapping("/user/resource/quota")
    public R syncUserQuota(){
        SyncUserResourceQuotaService.syncUser();
        return R.ok(true,"添加成功");
    }

    @Operation(summary = "同步登录区域")
    @PostMapping("/login/region")
    public R syncLoginRegion(){
        syncRegionService.syncLoginRegion();
        return R.ok(true,"同步成功");
    }

    @Operation(summary = "全量同步历史聊天记录到搜索引擎（MySQL -> ES）")
    @PostMapping("/chat/message/es")
    public R syncChatMessage2Es(){
        Long count = searchSyncService.syncChatMessage();
        return R.ok(count,"同步成功");
    }
}
