package com.xigua.api.service.admin;

import com.xigua.domain.dto.QueryBotPageDTO;
import com.xigua.domain.vo.admin.AdminBotPageVO;
import com.xigua.domain.vo.admin.AdminBotDetailVO;
import com.xigua.common.mybatis.base.BasePageVO;

/**
 * @ClassName AdminBotService
 * @Description 管理端机器人服务接口
 * @Author wangjinfei
 * @Date 2026/3/29
 */
public interface AdminBotService {

    /**
     * 分页查询机器人
     * @author wangjinfei
     * @date 2026/3/29
     * @param dto
     * @return BasePageVO<AdminBotPageVO>
     */
    BasePageVO<AdminBotPageVO> page(QueryBotPageDTO dto);

    /**
     * 获取机器人详情
     * @author wangjinfei
     * @date 2026/3/29
     * @param botId 机器人ID
     * @return AdminBotDetailVO
     */
    AdminBotDetailVO getDetail(String botId);
}
