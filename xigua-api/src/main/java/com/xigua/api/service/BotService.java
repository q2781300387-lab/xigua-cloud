package com.xigua.api.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.domain.dto.BotDTO;
import com.xigua.domain.entity.Bot;
import com.xigua.domain.vo.BotDetailVO;

import java.util.List;
import java.util.Set;

/**
 * @ClassName BotService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/10/1 15:23
 */
public interface BotService extends IService<Bot> {

    /**
     * 创建机器人
     * @author wangjinfei
     * @date 2025/10/1 15:29
     * @param dto
     * @return Boolean
    */
    Boolean create(BotDTO dto);

    /**
     * 更新机器人（仅允许创建者本人操作）
     * @param botId 机器人ID
     * @param dto 机器人资料
     * @param userId 当前登录用户ID
     * @return Boolean
     */
    Boolean update(String botId, BotDTO dto, String userId);

    /**
     * 根据用户id获取机器人数量
     * @author wangjinfei
     * @date 2025/10/1 16:45
     * @param userId
     * @return Integer
    */
    Integer getCountByUserId(String userId);

    /**
     * 根据用户id获取机器人id列表
     * @author wangjinfei
     * @date 2025/10/1 16:45
     * @param userId
     * @return Set<String>
    */
    Set<String> getBotIdsByUserId(String userId);

    /**
     * 获取机器人详情
     * @author wangjinfei
     * @date 2025/10/8 18:19
     * @param botId
     * @return BotDetailVO
    */
    BotDetailVO getBotDetail(String botId);

    /**
     * 获取机器人列表
     * @author wangjinfei
     * @date 2026/2/25 20:48
     * @param userId
     * @return List<Bot>
    */
    List<Bot> getBots(String userId);

    /**
     * 删除机器人（仅允许创建者本人操作）
     * @param botId 机器人ID
     * @param userId 当前登录用户ID
     * @return Boolean
     */
    Boolean delete(String botId, String userId);

    /**
     * 分页查询机器人（管理端）
     * @author wangjinfei
     * @date 2026/3/29
     * @param page 分页对象
     * @param name 机器人名称（模糊查询）
     * @param createBy 所属用户ID
     * @return Page<Bot>
     */
    Page<Bot> pageBot(Page<Bot> page, String name, String createBy);

    /**
     * 根据ID获取机器人（管理端）
     * @author wangjinfei
     * @date 2026/3/29
     * @param botId 机器人ID
     * @return Bot
     */
    Bot getById(String botId);
}
