package com.xigua.ai.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.ai.mapper.BotMapper;
import com.xigua.api.service.BotService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.DateUtil;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.bo.LastMessageContentBO;
import com.xigua.domain.dto.BotDTO;
import com.xigua.domain.entity.Bot;
import com.xigua.domain.enums.ChatType;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.vo.BotDetailVO;
import com.xigua.domain.vo.LastMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @ClassName BotServiceImpl
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/10/1 15:25
 */
@Slf4j
@Service
@DubboService
public class BotServiceImpl extends ServiceImpl<BotMapper, Bot> implements BotService {
    @Autowired
    private Sequence sequence;
    @Autowired
    private RedisClient redisClient;


    /**
     * 创建机器人
     * @author wangjinfei
     * @date 2025/10/1 15:29
     * @param dto
     * @return Boolean
     */
    @Override
    public Boolean create(BotDTO dto) {
        Bot bot = new Bot();
        if(StringUtils.isEmpty(dto.getName())){
            throw new BusinessException("机器人名称不能为空");
        }
        if(StringUtils.isEmpty(dto.getAvatar())){
            throw new BusinessException("机器人头像不能为空");
        }
        if(StringUtils.isEmpty(dto.getPrompt())){
            throw new BusinessException("机器人提示词不能为空");
        }
        BeanUtils.copyProperties(dto, bot);
        bot.setId(sequence.nextNo());
        bot.setUserId(UserContext.get().getUserId());
        baseMapper.insert(bot);

        // 缓存机器人信息
        redisClient.set(RedisEnum.BOT.getKey() + bot.getId(), JSONObject.toJSONString(bot));
        // 添加消息列表
        addLastMes(bot, UserContext.get().getUserId());

        return true;
    }

    /**
     * 更新机器人（仅允许创建者本人操作）
     */
    @Override
    public Boolean update(String botId, BotDTO dto, String userId) {
        if (StringUtils.isEmpty(botId)) {
            throw new BusinessException("机器人id不能为空");
        }
        if (dto == null) {
            throw new BusinessException("机器人资料不能为空");
        }
        Bot existing = baseMapper.selectById(botId);
        if (existing == null) {
            throw new BusinessException("机器人不存在");
        }
        if (StringUtils.isEmpty(userId) || !userId.equals(existing.getUserId())) {
            throw new BusinessException("无权修改该机器人");
        }
        if (StringUtils.isEmpty(dto.getName())) {
            throw new BusinessException("机器人名称不能为空");
        }
        if (StringUtils.isEmpty(dto.getAvatar())) {
            throw new BusinessException("机器人头像不能为空");
        }
        if (StringUtils.isEmpty(dto.getPrompt())) {
            throw new BusinessException("机器人提示词不能为空");
        }

        Bot update = new Bot();
        update.setId(botId);
        update.setName(dto.getName().trim());
        update.setAvatar(dto.getAvatar().trim());
        update.setDescription(dto.getDescription() == null ? "" : dto.getDescription().trim());
        update.setPrompt(dto.getPrompt());

        boolean updated = updateById(update);
        if (!updated) {
            return false;
        }

        Bot fresh = baseMapper.selectById(botId);
        if (fresh != null) {
            // 刷新详情缓存，详情页和聊天页立即拿到新名字/头像
            redisClient.set(RedisEnum.BOT.getKey() + fresh.getId(), JSONObject.toJSONString(fresh));
            // 同步会话列表里的机器人名称与头像，避免只改数据库导致聊天列表仍是旧资料
            Object lastMessage = redisClient.hashGet(RedisEnum.LAST_MES_CONTENT.getKey() + userId, botId);
            if (lastMessage != null) {
                JSONObject cached = JSONObject.parseObject(String.valueOf(lastMessage));
                cached.put("chatName", fresh.getName());
                cached.put("avatar", fresh.getAvatar());
                redisClient.hashPut(RedisEnum.LAST_MES_CONTENT.getKey() + userId, botId, JSONObject.toJSONString(cached));
            }
        }
        return true;
    }

    private void addLastMes(Bot bot, String userId){        // 映射vo
        LastMessageVO lastMessageVO = new LastMessageVO();
        lastMessageVO.setChatId(bot.getId());
        lastMessageVO.setChatType(ChatType.BOT.getType());
        lastMessageVO.setChatName(bot.getName());
        lastMessageVO.setAvatar(bot.getAvatar());
        LastMessageContentBO lastMessageContentBO = new LastMessageContentBO();
        lastMessageContentBO.setContent(String.format("你好！我是%s，很高兴认识你！", bot.getName()));
        lastMessageVO.setLastMessageContent(lastMessageContentBO);
        lastMessageVO.setUpdateTime(DateUtil.toEpochMilli(bot.getCreateTime()));

        // redis 最后消息
        redisClient.zsadd(RedisEnum.LAST_MES.getKey() + userId, bot.getId(), DateUtil.toEpochMilli(bot.getCreateTime()));
        // redis 最后消息内容
        redisClient.hashPut(RedisEnum.LAST_MES_CONTENT.getKey() + userId, bot.getId(), JSONObject.toJSONString(lastMessageVO));
    }

    /**
     * 根据用户id获取机器人数量
     * @author wangjinfei
     * @date 2025/10/1 16:45
     * @param userId
     * @return Integer
     */
    @Override
    public Integer getCountByUserId(String userId) {
        Integer count = baseMapper.getCountByUserId(userId);
        return count != null ? count : 0;
    }

    /**
     * 根据用户id获取机器人id列表
     * @author wangjinfei
     * @date 2025/10/1 16:45
     * @param userId
     * @return Set<String>
     */
    @Override
    public Set<String> getBotIdsByUserId(String userId) {
        return baseMapper.getBotIdsByUserId(userId);
    }

    /**
     * 获取机器人详情
     * @author wangjinfei
     * @date 2025/10/8 18:19
     * @param botId
     * @return BotDetailVO
     */
    @Override
    public BotDetailVO getBotDetail(String botId) {
        String botCache = redisClient.get(RedisEnum.BOT.getKey() + botId);
        BotDetailVO botDetailVO = null;
        if(StringUtils.isNotEmpty(botCache)){
            botDetailVO = JSONObject.parseObject(botCache, BotDetailVO.class);
        }else{
            // 缓存未命中时回查数据库并回填：直接写库导入的机器人没有缓存，
            // 只读缓存会让详情页永远拿不到数据
            Bot bot = baseMapper.selectById(botId);
            if (bot != null) {
                String botJson = JSONObject.toJSONString(bot);
                redisClient.set(RedisEnum.BOT.getKey() + botId, botJson);
                botDetailVO = JSONObject.parseObject(botJson, BotDetailVO.class);
            }
        }

        return botDetailVO;
    }

    @Override
    public List<Bot> getBots(String userId) {
        return baseMapper.selectList(new LambdaQueryWrapper<Bot>().eq(Bot::getUserId, userId).orderByDesc(Bot::getCreateTime));
    }

    /**
     * 删除机器人（仅允许创建者本人操作）
     */
    @Override
    public Boolean delete(String botId, String userId) {
        if (StringUtils.isEmpty(botId)) {
            throw new BusinessException("机器人id不能为空");
        }
        Bot bot = baseMapper.selectById(botId);
        if (bot == null) {
            throw new BusinessException("机器人不存在");
        }
        if (StringUtils.isEmpty(userId) || !userId.equals(bot.getUserId())) {
            throw new BusinessException("无权删除该机器人");
        }
        boolean removed = removeById(botId);
        if (removed) {
            // 清掉详情缓存，避免删除后仍能读到旧数据
            redisClient.del(RedisEnum.BOT.getKey() + botId);
        }
        return removed;
    }

    @Override
    public Page<Bot> pageBot(Page<Bot> page, String name, String createBy) {
        LambdaQueryWrapper<Bot> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(name)) {
            queryWrapper.like(Bot::getName, name);
        }
        if (StringUtils.isNotEmpty(createBy)) {
            queryWrapper.eq(Bot::getCreateBy, createBy);
        }
        queryWrapper.orderByDesc(Bot::getCreateTime);
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Bot getById(String botId) {
        if (StringUtils.isEmpty(botId)) {
            return null;
        }
        return baseMapper.selectById(botId);
    }
}
