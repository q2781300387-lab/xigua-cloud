package com.xigua.admin.controller;

import com.xigua.api.service.admin.CaptchaService;
import com.xigua.common.core.base.R;
import com.xigua.domain.vo.admin.CaptchaImageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName CaptchaController
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/21 21:32
 */
@Tag(name = "验证码接口")
@RequestMapping("/captcha")
@RestController
public class CaptchaController {
    @Autowired
    private CaptchaService captchaService;

    @Operation(summary = "获取验证码")
    @GetMapping("/image")
    public R<CaptchaImageVO> image(){
        return R.ok(captchaService.image(),"成功");
    }
}
