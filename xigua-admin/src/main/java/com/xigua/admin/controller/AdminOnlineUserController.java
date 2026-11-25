package com.xigua.admin.controller;

import com.xigua.api.service.admin.AdminOnlineUserService;
import com.xigua.common.core.base.R;
import com.xigua.domain.dto.QueryOnlineUserPageDTO;
import com.xigua.domain.vo.admin.AdminOnlineUserDetailVO;
import com.xigua.domain.vo.admin.AdminOnlineUserStatsVO;
import com.xigua.domain.vo.admin.AdminOnlineUserVO;
import com.xigua.domain.vo.admin.OnlineUserInfoVO;
import com.xigua.domain.vo.admin.OnlineUserLocationVO;
import com.xigua.domain.vo.admin.OnlineUserLocationResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName AdminOnlineUserController
 * @Description 管理端在线用户接口
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Tag(name = "管理端在线用户接口", description = "提供管理端在线用户查询功能")
@RequestMapping("/online-user")
@RestController
public class AdminOnlineUserController {

    @Autowired
    private AdminOnlineUserService adminOnlineUserService;

    @Operation(summary = "分页查询在线用户", description = "根据用户名等条件分页查询在线用户列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AdminOnlineUserVO.class)))
    })
    @PostMapping("/page")
    public R page(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "分页查询参数", required = true)
                  @RequestBody QueryOnlineUserPageDTO dto) {
        return R.ok(adminOnlineUserService.page(dto));
    }

    @Operation(summary = "获取在线用户统计", description = "获取总在线人数、Web端在线人数、移动端在线人数")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AdminOnlineUserStatsVO.class)))
    })
    @GetMapping("/stats")
    public R getStats() {
        return R.ok(adminOnlineUserService.getStats());
    }

    @Operation(summary = "获取在线用户详情", description = "获取在线用户的设备信息、连接信息和网络信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = AdminOnlineUserDetailVO.class)))
    })
    @GetMapping("/{userId}")
    public R getDetail(@Parameter(description = "用户ID", required = true) @PathVariable("userId") String userId) {
        return R.ok(adminOnlineUserService.getDetail(userId));
    }

    @Operation(summary = "获取在线用户地理位置分布", description = "获取所有在线用户的地理位置聚合信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = OnlineUserLocationResponseVO.class)))
    })
    @GetMapping("/locations")
    public R getOnlineUserLocations() {
        // TODO 本地开发环境模拟数据，生产环境请使用 adminOnlineUserService.getOnlineUserLocations()
//        List<OnlineUserLocationVO> mockData = new java.util.ArrayList<>();
//
//        // 北京
//        OnlineUserLocationVO beijing = new OnlineUserLocationVO();
//        beijing.setLat(39.9042);
//        beijing.setLng(116.4074);
//        beijing.setCountry("中国");
//        beijing.setCity("北京市");
//        beijing.setUserCount(3);
//
//        java.util.List<OnlineUserInfoVO> bjUsers = new java.util.ArrayList<>();
//        bjUsers.add(createMockUser("10001", "张三", "https://api.dicebear.com/7.x/avataaars/svg?seed=Zhang"));
//        bjUsers.add(createMockUser("10002", "李四", "https://api.dicebear.com/7.x/avataaars/svg?seed=Li"));
//        bjUsers.add(createMockUser("10003", "王五", "https://api.dicebear.com/7.x/avataaars/svg?seed=Wang"));
//        beijing.setUsers(bjUsers);
//        mockData.add(beijing);
//
//        // 上海
//        OnlineUserLocationVO shanghai = new OnlineUserLocationVO();
//        shanghai.setLat(31.2304);
//        shanghai.setLng(121.4737);
//        shanghai.setCountry("中国");
//        shanghai.setCity("上海市");
//        shanghai.setUserCount(2);
//
//        java.util.List<OnlineUserInfoVO> shUsers = new java.util.ArrayList<>();
//        shUsers.add(createMockUser("20001", "赵六", "https://api.dicebear.com/7.x/avataaars/svg?seed=Zhao"));
//        shUsers.add(createMockUser("20002", "孙七", "https://api.dicebear.com/7.x/avataaars/svg?seed=Sun"));
//        shanghai.setUsers(shUsers);
//        mockData.add(shanghai);
//
//        // 广州
//        OnlineUserLocationVO guangzhou = new OnlineUserLocationVO();
//        guangzhou.setLat(23.1291);
//        guangzhou.setLng(113.2644);
//        guangzhou.setCountry("中国");
//        guangzhou.setCity("广州市");
//        guangzhou.setUserCount(2);
//
//        java.util.List<OnlineUserInfoVO> gzUsers = new java.util.ArrayList<>();
//        gzUsers.add(createMockUser("30001", "周八", "https://api.dicebear.com/7.x/avataaars/svg?seed=Zhou"));
//        gzUsers.add(createMockUser("30002", "吴九", "https://api.dicebear.com/7.x/avataaars/svg?seed=Wu"));
//        guangzhou.setUsers(gzUsers);
//        mockData.add(guangzhou);
//
//        // 深圳
//        OnlineUserLocationVO shenzhen = new OnlineUserLocationVO();
//        shenzhen.setLat(22.5431);
//        shenzhen.setLng(114.0579);
//        shenzhen.setCountry("中国");
//        shenzhen.setCity("深圳市");
//        shenzhen.setUserCount(1);
//
//        java.util.List<OnlineUserInfoVO> szUsers = new java.util.ArrayList<>();
//        szUsers.add(createMockUser("40001", "郑十", "https://api.dicebear.com/7.x/avataaars/svg?seed=Zheng"));
//        shenzhen.setUsers(szUsers);
//        mockData.add(shenzhen);
//
//        OnlineUserLocationResponseVO response = new OnlineUserLocationResponseVO();
//        response.setOnlineUsers(mockData);

        return R.ok(adminOnlineUserService.getOnlineUserLocations());
    }

    private OnlineUserInfoVO createMockUser(String userId, String username, String avatar) {
        OnlineUserInfoVO user = new OnlineUserInfoVO();
        user.setUserId(userId);
        user.setUsername(username);
        user.setAvatar(avatar);
        user.setOnlineStatus("ONLINE");
        return user;
    }
}
