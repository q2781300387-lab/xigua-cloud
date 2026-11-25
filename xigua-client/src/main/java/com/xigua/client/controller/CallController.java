package com.xigua.client.controller;

import com.xigua.api.service.CallService;
import com.xigua.common.core.base.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName CallController
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/1/13 21:29
 */
@Tag(name = "呼叫接口")
@Slf4j
@RequestMapping("/call")
@RestController
public class CallController {
    @Autowired
    private CallService callService;

    @Operation(summary = "创建呼叫id")
    @PostMapping("/createCallId")
    public R<String> createCallId(){
        return R.ok(callService.createCallId());
    }
}
