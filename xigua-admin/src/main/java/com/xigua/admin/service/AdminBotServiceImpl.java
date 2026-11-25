package com.xigua.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xigua.api.service.BotService;
import com.xigua.api.service.UserService;
import com.xigua.api.service.admin.AdminBotService;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.dto.QueryBotPageDTO;
import com.xigua.domain.entity.Bot;
import com.xigua.domain.entity.User;
import com.xigua.domain.vo.admin.AdminBotPageVO;
import com.xigua.domain.vo.admin.AdminBotDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName AdminBotServiceImpl
 * @Description 管理端机器人服务实现
 * @Author wangjinfei
 * @Date 2026/3/29
 */
@Slf4j
@Service
@DubboService
public class AdminBotServiceImpl implements AdminBotService {

    @DubboReference
    private BotService botService;

    @DubboReference
    private UserService userService;

    @Override
    public BasePageVO<AdminBotPageVO> page(QueryBotPageDTO dto) {
        // 分页查询
        Page<Bot> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        Page<Bot> resultPage = botService.pageBot(page, dto.getName(), dto.getCreateBy());

        // 批量查询用户信息
        Map<String, User> userMap = getUserMap(resultPage.getRecords());

        // 转换为VO
        List<AdminBotPageVO> voList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(resultPage.getRecords())) {
            voList = resultPage.getRecords().stream()
                    .map(bot -> convertToVO(bot, userMap))
                    .collect(Collectors.toList());
        }

        // 构建返回结果
        BasePageVO<AdminBotPageVO> result = new BasePageVO<>();
        result.setPageNum(dto.getPageNum());
        result.setPageSize(dto.getPageSize());
        result.setRows(voList);
        result.setTotalCount(resultPage.getTotal());
        result.setTotalPage(resultPage.getPages());

        return result;
    }

    /**
     * 获取用户信息Map
     * @author wangjinfei
     * @date 2026/3/29
     * @param bots 机器人列表
     * @return Map<String, User>
     */
    private Map<String, User> getUserMap(List<Bot> bots) {
        if (CollectionUtils.isEmpty(bots)) {
            return Map.of();
        }

        List<String> userIds = bots.stream()
                .map(Bot::getCreateBy)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(userIds)) {
            return Map.of();
        }

        List<User> users = userService.getListByIds(userIds);
        if (CollectionUtils.isEmpty(users)) {
            return Map.of();
        }

        return users.stream()
                .collect(Collectors.toMap(User::getId, user -> user, (u1, u2) -> u1));
    }

    /**
     * 转换为VO
     * @author wangjinfei
     * @date 2026/3/29
     * @param bot 机器人
     * @param userMap 用户Map
     * @return AdminBotPageVO
     */
    private AdminBotPageVO convertToVO(Bot bot, Map<String, User> userMap) {
        AdminBotPageVO vo = new AdminBotPageVO();
        vo.setId(bot.getId());
        vo.setName(bot.getName());
        vo.setAvatar(bot.getAvatar());
        vo.setCreateTime(bot.getCreateTime());

        // 填充所属用户信息
        if (bot.getCreateBy() != null && userMap.containsKey(bot.getCreateBy())) {
            User user = userMap.get(bot.getCreateBy());
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setUserAvatar(user.getAvatar());
        }

        return vo;
    }

    @Override
    public AdminBotDetailVO getDetail(String botId) {
        if (StringUtils.isEmpty(botId)) {
            return null;
        }

        Bot bot = botService.getById(botId);
        if (bot == null) {
            return null;
        }

        AdminBotDetailVO vo = new AdminBotDetailVO();
        vo.setId(bot.getId());
        vo.setName(bot.getName());
        vo.setAvatar(bot.getAvatar());
        vo.setDescription(bot.getDescription());
        vo.setPrompt(bot.getPrompt());
        vo.setCreateTime(bot.getCreateTime());

        // 查询所属用户信息
        if (StringUtils.isNotEmpty(bot.getCreateBy())) {
            User user = userService.getById(bot.getCreateBy());
            if (user != null) {
                vo.setUserId(user.getId());
                vo.setUsername(user.getUsername());
                vo.setUserAvatar(user.getAvatar());
            }
        }

        return vo;
    }
}
