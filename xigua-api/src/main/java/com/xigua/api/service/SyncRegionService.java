package com.xigua.api.service;

/**
 * 同步区域服务接口
 * 用于补充历史登录日志的区域信息
 *
 * @ClassName SyncRegionService
 * @Description
 * @Author wangjinfei
 * @Date 2026/3/9 17:08
 */
public interface SyncRegionService {

    /**
     * 同步登录日志的区域信息
     * 根据 login_ip 补充 login_region 字段
     *
     * @return 同步的记录数
     */
    int syncLoginRegion();

    /**
     * 同步指定登录日志的区域信息
     *
     * @param id 登录日志ID
     * @return 是否同步成功
     */
    boolean syncLoginRegionById(String id);
}
