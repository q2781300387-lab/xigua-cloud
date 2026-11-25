package com.xigua.api.service.admin;

import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.dto.QueryGroupPageDTO;
import com.xigua.domain.dto.QueryGroupMemberPageDTO;
import com.xigua.domain.vo.admin.AdminGroupDetailVO;
import com.xigua.domain.vo.admin.AdminGroupMemberPageVO;
import com.xigua.domain.vo.admin.AdminGroupPageVO;
import com.xigua.domain.vo.admin.AdminGroupStatisticsVO;

/**
 * @ClassName AdminGroupService
 * @Description 管理端群组服务接口
 * @Author wangjinfei
 * @Date 2026/3/7
 */
public interface AdminGroupService {

    /**
     * 分页查询群组
     * @author wangjinfei
     * @date 2026/3/7
     * @param dto
     * @return BasePageVO<AdminGroupPageVO>
     */
    BasePageVO<AdminGroupPageVO> page(QueryGroupPageDTO dto);

    /**
     * 查询群组详情
     * @author wangjinfei
     * @date 2026/3/10
     * @param groupId
     * @return AdminGroupDetailVO
     */
    AdminGroupDetailVO getDetail(String groupId);

    /**
     * 获取群组统计信息
     * @author wangjinfei
     * @date 2026/3/10
     * @return AdminGroupStatisticsVO
     */
    AdminGroupStatisticsVO getStatistics(String groupId);

    /**
     * 群成员分页查询
     * @author wangjinfei
     * @date 2026/3/10
     * @param groupId
     * @param dto
     * @return BasePageVO<AdminGroupMemberPageVO>
     */
    BasePageVO<AdminGroupMemberPageVO> pageMembers(String groupId, QueryGroupMemberPageDTO dto);

    /**
     * 管理端设置全员禁言
     */
    Boolean setMuteAll(String groupId, Integer muteAll);

    /**
     * 管理端解散群聊
     */
    Boolean disbandGroup(String groupId);

    /**
     * 管理端移出群成员
     */
    Boolean removeMember(String groupId, String userId);
}
