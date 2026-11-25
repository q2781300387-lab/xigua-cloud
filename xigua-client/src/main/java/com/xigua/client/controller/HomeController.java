package com.xigua.client.controller;

import com.xigua.api.service.HomeService;
import com.xigua.client.utils.IpUtil;
import com.xigua.client.ws.WebSocketTest;
import com.xigua.common.core.util.NetUtil;
import com.xigua.common.core.base.R;
import com.xigua.domain.vo.HomeCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName HomeController
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/8/9 11:05
 */
@Tag(name = "首页接口")
@RequestMapping("/home")
@RestController
public class HomeController {
    @Autowired
    private HomeService homeService;

    /**
     * 获取首页统计信息
     * @author wangjinfei
     * @date 2025/8/9 11:06
     * @return HomeCountVO
     */
    @ApiResponses({@ApiResponse(responseCode = "200", description = "查询成功", content =
            { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = HomeCountVO.class)) })})
    @Operation(summary = "获取首页统计信息")
    @GetMapping("/getHomeCount")
    public R<HomeCountVO> getHomeCount(){
        return R.ok(homeService.getHomeCount());
    }

    @Operation(summary = "获取ip")
    @GetMapping("/ip")
    public R<Map<String, String>> getIp(HttpServletRequest request){
        Map<String, String> map = new HashMap<>();
        String remoteAddr = request.getRemoteAddr();
        map.put("remoteAddr", remoteAddr);
        map.put("ip1", NetUtil.getClientIp());
        map.put("ip2", NetUtil.getClientIp(request));
        map.put("ip3", IpUtil.getClientIp(request));
        return R.ok(map);
    }

    @Operation(summary = "测试连接")
    @GetMapping("/connect")
    public void connect(@RequestParam("count") int count) throws InterruptedException {
        WebSocketTest.connect(count);
    }
}
