package com.xigua.api.service.admin;

import com.xigua.domain.dto.QueryUserPageDTO;
import com.xigua.domain.dto.UpdateUserDTO;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.vo.admin.ActivityStatsVO;
import com.xigua.domain.vo.admin.AdminUserDetailVO;
import com.xigua.domain.vo.admin.AdminUserPage;

/**
 * @ClassName AdminUserService
 * @Description 管理端用户服务接口
 * @Author wangjinfei
 * @Date 2026/2/1 17:12
 */
public interface AdminUserService {

    /**
     * 分页查询用户
     * @author wangjinfei
     * @date 2026/2/1 17:12
     * @param dto
     * @return BasePageVO<AdminUserPage>
     */
    BasePageVO<AdminUserPage> page(QueryUserPageDTO dto);

    /**
     * 根据ID查询用户详情
     * @author wangjinfei
     * @date 2026/3/10
     * @param userId 用户ID
     * @return AdminUserDetailVO
     */
    AdminUserDetailVO getDetail(String userId);

    /**
     * 获取活跃统计数据
     * @author wangjinfei
     * @date 2026/3/10
     * @param userId 用户ID
     * @return ActivityStatsVO
     */
    ActivityStatsVO getActivityStats(String userId);

    /**
     * 更新用户信息
     * @author wangjinfei
     * @date 2026/3/10
     * @param dto 用户更新信息
     * @return Boolean
     */
    Boolean updateUser(UpdateUserDTO dto);

    /**
     * 封禁账号（永久封禁）
     */
    Boolean banUser(String userId, String reason);

    /**
     * 解除封禁/禁言，恢复为正常状态
     */
    Boolean unbanUser(String userId);

    /**
     * 禁言账号；durationHours 为空或 0 表示永久禁言
     */
    Boolean muteUser(String userId, Integer durationHours, String reason);

    /**
     * 解除禁言
     */
    Boolean unmuteUser(String userId);
}
