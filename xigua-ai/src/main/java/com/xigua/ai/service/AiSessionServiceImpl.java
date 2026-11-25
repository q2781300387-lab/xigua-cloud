package com.xigua.ai.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.ai.helper.TitleGenerator;
import com.xigua.ai.mapper.AiSessionMapper;
import com.xigua.api.service.AiMessageService;
import com.xigua.api.service.AiSessionService;
import com.xigua.api.service.BotService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.dto.CreateSessionRequest;
import com.xigua.domain.dto.PageSessionsDTO;
import com.xigua.domain.entity.AiSession;
import com.xigua.domain.entity.Bot;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.util.BasePage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName AiSessionServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/24 21:14
 */
@Service
public class AiSessionServiceImpl extends ServiceImpl<AiSessionMapper, AiSession> implements AiSessionService {
    @Autowired
    private Sequence sequence;
    @Autowired
    private TitleGenerator titleGenerator;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private BotService botService;
    @Autowired
    private AiMessageService aiMessageService;

    @Override
    public AiSession createSession(CreateSessionRequest request) {
        if(StringUtils.isEmpty(request.getUserId())){
            throw new BusinessException("用户id不能为空");
        }
        if(StringUtils.isEmpty(request.getBotId())){
            throw new BusinessException("机器人id不能为空");
        }

        // 会话归属只认当前登录人，不能由请求体指定成别人
        String currentUserId = currentUserId();
        request.setUserId(currentUserId);

        Bot bot = redisClient.get(RedisEnum.BOT.getKey() + request.getBotId(), Bot.class);
        if(bot == null){
            // 缓存会随重启/过期整体丢失，只读缓存会把库里有记录的机器人误判成“不存在”，
            // 导致所有机器人会话都建不起来，这里回查一次数据库并回填
            bot = botService.getById(request.getBotId());
            if(bot == null){
                throw new BusinessException("机器人不存在");
            }
            redisClient.set(RedisEnum.BOT.getKey() + request.getBotId(), JSONObject.toJSONString(bot));
        }

        AiSession aiSession = new AiSession();
        String id = sequence.nextNo();
        aiSession.setId(id);
        aiSession.setBotId(request.getBotId());
        aiSession.setUserId(request.getUserId());
        // 支持按会话选择模型，未指定时用系统默认模型
        String model = request.getModel();
        if (StringUtils.isEmpty(model)) {
            model = "glm-4-flash";
        }
        aiSession.setModel(model);
        aiSession.setTitle(getSessionTitle(request.getInput()));
        aiSession.setSystemPrompt(bot.getPrompt());
        baseMapper.insert(aiSession);

        // 保存系统提示词
        aiMessageService.saveSystemMessage(id, bot.getPrompt());

        // 缓存
        redisClient.set(RedisEnum.AI_SESSION.getKey() + id, JSONObject.toJSONString(aiSession));

        return aiSession;
    }

    @Override
    public AiSession getOwnedSession(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            throw new BusinessException("会话id不能为空");
        }
        AiSession aiSession = baseMapper.selectById(sessionId);
        if (aiSession == null) {
            throw new BusinessException("会话不存在");
        }
        if (!currentUserId().equals(aiSession.getUserId())) {
            throw new BusinessException("无权访问该会话");
        }
        return aiSession;
    }

    private String currentUserId() {
        if (UserContext.get() == null || StringUtils.isEmpty(UserContext.get().getUserId())) {
            throw new BusinessException("请先登录");
        }
        return UserContext.get().getUserId();
    }

    @Override
    public boolean deleteSession(String sessionId) {
        getOwnedSession(sessionId);
        return baseMapper.deleteById(sessionId) > 0;
    }

    @Override
    public BasePageVO<AiSession> pageSessions(PageSessionsDTO dto) {
        dto.setUserId(UserContext.get().getUserId());

        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        Page<AiSession> page = new Page<>(pageNum, pageSize);
        List<AiSession> aiSessions = baseMapper.pageSessions(page, dto);
        if(CollectionUtils.isEmpty(aiSessions)){
            return BasePage.emptyResult(page);
        }
        return BasePage.getResult(page, aiSessions);
    }

    @Override
    public boolean renameSession(String sessionId, String title) {
        if (StringUtils.isEmpty(title)) {
            throw new BusinessException("会话名称不能为空");
        }
        if (title.length() > 15) {
            throw new BusinessException("会话名称最多15个字符");
        }
        AiSession aiSession = getOwnedSession(sessionId);
        aiSession.setTitle(title);
        int rows = baseMapper.updateById(aiSession);
        if (rows > 0) {
            redisClient.set(RedisEnum.AI_SESSION.getKey() + sessionId, JSONObject.toJSONString(aiSession));
        }
        return rows > 0;
    }

    @Override
    public boolean updateModel(String sessionId, String model) {
        if (StringUtils.isEmpty(model)) {
            throw new BusinessException("模型名称不能为空");
        }
        AiSession aiSession = getOwnedSession(sessionId);
        aiSession.setModel(model);
        int rows = baseMapper.updateById(aiSession);
        if (rows > 0) {
            redisClient.set(RedisEnum.AI_SESSION.getKey() + sessionId, JSONObject.toJSONString(aiSession));
        }
        return rows > 0;
    }

    private String getSessionTitle(String input){
        return titleGenerator.title(input);
    }
}
