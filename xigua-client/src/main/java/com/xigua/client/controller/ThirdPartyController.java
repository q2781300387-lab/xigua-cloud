package com.xigua.client.controller;

import com.xigua.client.utils.IpUtil;
import com.xigua.common.core.base.R;
import com.xigua.domain.thirdparty.GetLocationRes;
import com.xigua.domain.thirdparty.GetWeatherRes;
import com.xigua.api.service.ThirdPartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName ThirdPartyController
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/11 14:28
 */
@Tag(name = "三方接口")
@RequestMapping("/thirdParty")
@RestController
public class ThirdPartyController {
    @Autowired
    private ThirdPartyService thirdPartyService;

    /**
     * 根据ip获取地址
     * @author wangjinfei
     * @date 2025/5/11 14:06
     * @param request
     * @return String
     */
    @ApiResponses({@ApiResponse(responseCode = "200", description = "查询成功", content =
            { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = GetLocationRes.class)) })})
    @Operation(summary = "根据ip获取地址")
    @GetMapping("/getLocation")
    public R<GetLocationRes> getLocation(HttpServletRequest request){
        String ip = IpUtil.getClientIp(request);
        return R.ok(thirdPartyService.getLocation(ip));
    }

    /**
     * 根据城市获取天气
     * @author wangjinfei
     * @date 2025/5/11 15:29
     * @param city
     * @return GetWeatherRes
     */
    @ApiResponses({@ApiResponse(responseCode = "200", description = "查询成功", content =
            { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = GetWeatherRes.class)) })})
    @Operation(summary = "根据城市获取天气")
    @PostMapping("/getWeather")
    public R<GetWeatherRes> getWeather(@RequestParam("city") String city){
        return R.ok(thirdPartyService.getWeather(city));
    }
}
