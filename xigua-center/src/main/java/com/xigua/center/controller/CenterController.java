package com.xigua.center.controller;

import com.xigua.api.service.CenterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName CenterController
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/4/23 20:15
 */
@Tag(name = "长连接中心接口")
@RequestMapping("/center")
@RestController
public class CenterController {
    @Autowired
    private CenterService centerService;
}
