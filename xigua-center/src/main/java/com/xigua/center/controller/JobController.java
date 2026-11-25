package com.xigua.center.controller;

import com.xigua.api.service.JobService;
import com.xigua.center.service.MessageArchiveService;
import com.xigua.common.core.base.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName JonController
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/1/6 11:27
 */
@Tag(name = "定时任务接口")
@RequestMapping("/job")
@RestController
public class JobController {
    @Autowired
    private JobService jobService;
    @Autowired
    private MessageArchiveService messageArchiveService;

    /**
     * 检查在线连接
     * @author wangjinfei
     * @date 2025/6/16 20:36
     */
    @Operation(summary = "检查在线连接")
    @PostMapping("/checkOnlineConnection")
    void checkOnlineConnection(){
        jobService.checkOnlineConnection();
    }

    @Operation(summary = "手动触发消息归档")
    @PostMapping("/archiveMessages")
    public R<Integer> archiveMessages(@RequestParam(defaultValue = "180") Integer days) {
        return R.ok(messageArchiveService.archive(days), "归档完成");
    }
}
