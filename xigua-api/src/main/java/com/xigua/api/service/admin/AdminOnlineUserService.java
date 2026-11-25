package com.xigua.api.service.admin;

import com.xigua.domain.dto.QueryOnlineUserPageDTO;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.vo.admin.AdminOnlineUserDetailVO;
import com.xigua.domain.vo.admin.AdminOnlineUserStatsVO;
import com.xigua.domain.vo.admin.AdminOnlineUserVO;
import com.xigua.domain.vo.admin.OnlineUserLocationResponseVO;

/**
 * @ClassName AdminOnlineUserService
 * @Description 管理端在线用户服务接口
 * @Author wangjinfei
 * @Date 2026/3/10
 */
public interface AdminOnlineUserService {

    /**
     * 分页查询在线用户
     * @author wangjinfei
     * @date 2026/3/10
     * @param dto
     * @return BasePageVO<AdminOnlineUserVO>
     */
    BasePageVO<AdminOnlineUserVO> page(QueryOnlineUserPageDTO dto);

    /**
     * 获取在线用户统计
     * @author wangjinfei
     * @date 2026/3/10
     * @return AdminOnlineUserStatsVO
     */
    AdminOnlineUserStatsVO getStats();

    /**
     * 获取在线用户详情
     * @author Claude
     * @date 2026/3/20
     * @param userId 用户ID
     * @return AdminOnlineUserDetailVO
     */
    AdminOnlineUserDetailVO getDetail(String userId);

    /**
     * 获取在线用户地理位置分布
     * @author wangjinfei
     * @date 2026/3/29
     * @return OnlineUserLocationResponseVO
     */
    OnlineUserLocationResponseVO getOnlineUserLocations();
}
