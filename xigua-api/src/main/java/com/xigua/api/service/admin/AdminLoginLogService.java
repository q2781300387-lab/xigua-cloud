package com.xigua.api.service.admin;

import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.dto.QueryLoginLogPageDTO;

/**
 * @ClassName AdminLoginLogService
 * @Description 管理端登录日志服务接口
 * @Author wangjinfei
 * @Date 2026/3/8
 */
public interface AdminLoginLogService {

    /**
     * 分页查询登录日志
     * @author wangjinfei
     * @date 2026/3/8
     * @param dto
     * @return BasePageVO<AdminLoginLogPageVO>
     */
    BasePageVO<com.xigua.domain.vo.admin.AdminLoginLogPageVO> page(QueryLoginLogPageDTO dto);
}
