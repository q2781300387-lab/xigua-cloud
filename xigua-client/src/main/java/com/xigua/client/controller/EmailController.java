package com.xigua.client.controller;

import com.xigua.common.core.base.R;
import com.xigua.domain.dto.VerifyResetCodeDTO;
import com.xigua.domain.vo.VerifyResetCodeVO;
import com.xigua.api.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName EmailController
 * @Description
 * @Author wangjinfei
 * @Date 2025/5/7 14:52
 */
@Tag(name = "邮箱接口")
@RequestMapping("/email")
@RestController
public class EmailController {
    @Autowired
    private EmailService emailService;

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/send")
    public R send(@RequestParam("email") String email){
        Boolean flag = emailService.send(email, "", "");
        if(flag){
            return R.ok(null, "发送成功");
        }
        return R.fail("发送失败");
    }

    /**
     * 验证重置密码验证码
     * @author wangjinfei
     * @date 2026/4/13
     * @param dto
     * @return VerifyResetCodeVO
     */
    @ApiResponses({@ApiResponse(responseCode = "200", description = "验证成功", content =
            { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = VerifyResetCodeVO.class)) })})
    @Operation(summary = "验证重置密码验证码")
    @PostMapping("/verifyResetCode")
    public R<VerifyResetCodeVO> verifyResetCode(@Validated @RequestBody VerifyResetCodeDTO dto){
        String resetToken = emailService.verifyResetCode(dto.getEmail(), dto.getCode());
        VerifyResetCodeVO vo = new VerifyResetCodeVO(resetToken);
        return R.ok(vo, "验证成功");
    }
}
