package com.xigua.center.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xigua.api.service.BotService;
import com.xigua.api.service.CenterService;
import com.xigua.api.service.ChatMessageService;
import com.xigua.api.service.GroupMemberService;
import com.xigua.api.service.GroupService;
import com.xigua.api.service.UserService;
import com.xigua.center.mapper.MessageFavoriteMapper;
import com.xigua.center.mapper.MessageReactionMapper;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.dto.MessageFavoriteDTO;
import com.xigua.domain.dto.MessageReactionDTO;
import com.xigua.domain.dto.MessageReactionQueryDTO;
import com.xigua.domain.entity.ChatMessage;
import com.xigua.domain.entity.Group;
import com.xigua.domain.entity.MessageFavorite;
import com.xigua.domain.entity.MessageReaction;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.ChatType;
import com.xigua.domain.enums.MessageSubType;
import com.xigua.domain.enums.MessageType;
import com.xigua.domain.enums.Sender;
import com.xigua.domain.vo.BotDetailVO;
import com.xigua.domain.vo.MessageFavoriteVO;
import com.xigua.domain.vo.MessageInteractionVO;
import com.xigua.domain.vo.MessageReactionItemVO;
import com.xigua.domain.vo.MessageReactionSummaryVO;
import com.xigua.domain.ws.MessageResponse;
import com.xigua.domain.connect.Client;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息互动：表情回应与收藏。
 * 两张表都只存关系/快照，不修改 xg_chat_message，避免影响消息主链路。
 */
@Slf4j
@Service
public class MessageInteractionService {

    /** 前端固定展示的 8 个表情，后端也做白名单，防止任意文本写进回应栏 */
    private static final Set<String> ALLOWED_EMOJIS = Set.of("👍", "❤️", "😂", "😮", "😢", "👏", "🔥", "🎉");

    @Autowired
    private MessageReactionMapper reactionMapper;
    @Autowired
    private MessageFavoriteMapper favoriteMapper;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private CenterService centerService;
    @Autowired
    private Sequence sequence;
    @DubboReference(check = false, timeout = 3000)
    private GroupMemberService groupMemberService;
    @DubboReference(check = false, timeout = 3000)
    private GroupService groupService;
    @DubboReference(check = false, timeout = 3000)
    private UserService userService;
    @DubboReference(check = false, timeout = 3000)
    private BotService botService;

    /**
     * 一次拉取当前屏消息的回应和收藏状态，避免前端逐条请求。
     */
    public MessageInteractionVO interactions(MessageReactionQueryDTO dto) {
        String userId = currentUserId();
        MessageInteractionVO result = new MessageInteractionVO();
        List<String> ids = dto == null || dto.getMessageIds() == null ? Collections.emptyList()
                : dto.getMessageIds().stream().filter(StringUtils::isNotBlank).distinct().limit(80).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return result;
        }
        List<MessageReaction> rows = reactionMapper.selectList(
                new LambdaQueryWrapper<MessageReaction>().in(MessageReaction::getMessageId, ids));
        result.setReactions(buildSummaries(ids, rows, userId));
        List<MessageFavorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<MessageFavorite>()
                        .eq(MessageFavorite::getUserId, userId)
                        .in(MessageFavorite::getMessageId, ids));
        result.setFavoriteIds(favorites.stream().map(MessageFavorite::getMessageId).collect(Collectors.toList()));
        return result;
    }

    /**
     * 切换回应：同一用户同一条消息只保留一个表情；再次点同一个表情即取消。
     */
    @Transactional
    public MessageReactionSummaryVO toggleReaction(MessageReactionDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getMessageId())) {
            throw new BusinessException("消息不能为空");
        }
        String emoji = StringUtils.isBlank(dto.getEmoji()) ? "" : dto.getEmoji().trim();
        if (!ALLOWED_EMOJIS.contains(emoji)) {
            throw new BusinessException("暂不支持这个表情");
        }
        String userId = currentUserId();
        ChatMessage message = accessibleMessage(dto.getMessageId(), userId);

        MessageReaction existing = reactionMapper.selectOne(
                new LambdaQueryWrapper<MessageReaction>()
                        .eq(MessageReaction::getMessageId, message.getId())
                        .eq(MessageReaction::getUserId, userId)
                        .last("limit 1"));
        boolean active;
        if (existing == null) {
            MessageReaction row = new MessageReaction();
            row.setId(sequence.nextNo());
            row.setMessageId(message.getId());
            row.setUserId(userId);
            row.setEmoji(emoji);
            row.setCreateTime(LocalDateTime.now());
            row.setUpdateTime(row.getCreateTime());
            reactionMapper.insert(row);
            active = true;
        } else if (emoji.equals(existing.getEmoji())) {
            reactionMapper.deleteById(existing.getId());
            active = false;
        } else {
            existing.setEmoji(emoji);
            existing.setUpdateTime(LocalDateTime.now());
            reactionMapper.updateById(existing);
            active = true;
        }

        List<MessageReaction> rows = reactionMapper.selectList(
                new LambdaQueryWrapper<MessageReaction>().eq(MessageReaction::getMessageId, message.getId()));
        MessageReactionSummaryVO summary = buildSummary(message.getId(), rows, userId);
        notifyReaction(message, userId, emoji, active);
        return summary;
    }

    /**
     * 收藏 / 取消收藏。收藏时落一份消息快照，原消息之后撤回或删除也能在收藏夹查看。
     */
    @Transactional
    public Boolean toggleFavorite(MessageFavoriteDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getMessageId())) {
            throw new BusinessException("消息不能为空");
        }
        String userId = currentUserId();
        ChatMessage message = accessibleMessage(dto.getMessageId(), userId);

        MessageFavorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<MessageFavorite>()
                        .eq(MessageFavorite::getUserId, userId)
                        .eq(MessageFavorite::getMessageId, message.getId())
                        .last("limit 1"));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            return false;
        }

        MessageFavorite favorite = new MessageFavorite();
        favorite.setId(sequence.nextNo());
        favorite.setUserId(userId);
        favorite.setMessageId(message.getId());
        favorite.setChatType(message.getChatType());
        favorite.setPeerId(message.getReceiverId());
        favorite.setChatName(resolveChatName(message, userId));
        favorite.setSenderId(message.getSenderId());
        favorite.setSenderName(resolveSenderName(message.getSenderId(), userId));
        favorite.setMessageType(message.getMessageType());
        favorite.setContent(message.getMessage() == null ? "" : message.getMessage());
        favorite.setOriginalTime(message.getCreateTime());
        favorite.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        return true;
    }

    /**
     * 收藏夹分页列表，支持关键词与消息类型筛选。
     */
    public BasePageVO<MessageFavoriteVO> favorites(MessageFavoriteDTO dto) {
        String userId = currentUserId();
        int pageNum = dto == null || dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
        int pageSize = dto == null || dto.getPageSize() == null ? 20 : Math.min(Math.max(dto.getPageSize(), 1), 50);
        LambdaQueryWrapper<MessageFavorite> query = new LambdaQueryWrapper<MessageFavorite>()
                .eq(MessageFavorite::getUserId, userId);
        if (dto != null && dto.getMessageType() != null) {
            query.eq(MessageFavorite::getMessageType, dto.getMessageType());
        }
        if (dto != null && StringUtils.isNotBlank(dto.getKeyword())) {
            String keyword = dto.getKeyword().trim();
            query.and(w -> w.like(MessageFavorite::getContent, keyword)
                    .or().like(MessageFavorite::getChatName, keyword)
                    .or().like(MessageFavorite::getSenderName, keyword));
        }
        query.orderByDesc(MessageFavorite::getCreateTime);
        Page<MessageFavorite> page = favoriteMapper.selectPage(new Page<>(pageNum, pageSize), query);
        List<MessageFavoriteVO> rows = page.getRecords().stream().map(this::toFavoriteVO).collect(Collectors.toList());
        BasePageVO<MessageFavoriteVO> result = new BasePageVO<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotalCount(page.getTotal());
        result.setTotalPage(page.getPages());
        result.setRows(rows);
        return result;
    }

    /**
     * 直接移除收藏，供收藏夹左滑/删除使用。
     */
    public Boolean removeFavorite(String messageId) {
        if (StringUtils.isBlank(messageId)) {
            throw new BusinessException("消息不能为空");
        }
        favoriteMapper.delete(new LambdaQueryWrapper<MessageFavorite>()
                .eq(MessageFavorite::getUserId, currentUserId())
                .eq(MessageFavorite::getMessageId, messageId));
        return true;
    }

    private List<MessageReactionSummaryVO> buildSummaries(List<String> ids, List<MessageReaction> rows, String userId) {
        Map<String, List<MessageReaction>> grouped = rows.stream()
                .collect(Collectors.groupingBy(MessageReaction::getMessageId, LinkedHashMap::new, Collectors.toList()));
        List<MessageReactionSummaryVO> result = new ArrayList<>();
        for (String id : ids) {
            result.add(buildSummary(id, grouped.getOrDefault(id, Collections.emptyList()), userId));
        }
        return result;
    }

    private MessageReactionSummaryVO buildSummary(String messageId, List<MessageReaction> rows, String userId) {
        MessageReactionSummaryVO summary = new MessageReactionSummaryVO();
        summary.setMessageId(messageId);
        if (CollectionUtils.isEmpty(rows)) {
            return summary;
        }
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Boolean> mine = new HashMap<>();
        for (MessageReaction row : rows) {
            counts.merge(row.getEmoji(), 1, Integer::sum);
            if (StringUtils.equals(row.getUserId(), userId)) {
                mine.put(row.getEmoji(), true);
            }
        }
        List<MessageReactionItemVO> items = new ArrayList<>();
        counts.forEach((emoji, count) -> {
            MessageReactionItemVO item = new MessageReactionItemVO();
            item.setEmoji(emoji);
            item.setCount(count);
            item.setMine(mine.getOrDefault(emoji, false));
            items.add(item);
        });
        items.sort((a, b) -> b.getCount().compareTo(a.getCount()));
        summary.setItems(items);
        return summary;
    }

    private MessageFavoriteVO toFavoriteVO(MessageFavorite entity) {
        MessageFavoriteVO vo = new MessageFavoriteVO();
        vo.setId(entity.getId());
        vo.setMessageId(entity.getMessageId());
        vo.setChatType(entity.getChatType());
        vo.setPeerId(entity.getPeerId());
        vo.setChatName(entity.getChatName());
        vo.setSenderId(entity.getSenderId());
        vo.setSenderName(entity.getSenderName());
        vo.setMessageType(entity.getMessageType());
        vo.setContent(entity.getContent());
        vo.setOriginalTime(entity.getOriginalTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /**
     * 消息可见性校验：群消息必须是群成员，单聊必须是双方之一，机器人对话必须是本人。
     */
    private ChatMessage accessibleMessage(String messageId, String userId) {
        ChatMessage message = chatMessageService.getById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        if (message.getWithdrawn() != null && message.getWithdrawn() == 1) {
            throw new BusinessException("消息已撤回");
        }
        if (message.getMessageType() != null && message.getMessageType() == 6) {
            throw new BusinessException("系统消息不支持该操作");
        }
        Integer chatType = message.getChatType();
        if (chatType != null && chatType == ChatType.GROUP.getType()) {
            Set<String> members = groupMemberService.getGroupMembers(message.getReceiverId());
            if (CollectionUtils.isEmpty(members) || !members.contains(userId)) {
                throw new BusinessException("你不在该群聊中");
            }
        } else if (chatType != null && chatType == ChatType.BOT.getType()) {
            if (!StringUtils.equals(message.getSenderId(), userId)) {
                throw new BusinessException("无权操作该消息");
            }
        } else if (!StringUtils.equals(message.getSenderId(), userId)
                && !StringUtils.equals(message.getReceiverId(), userId)) {
            throw new BusinessException("无权操作该消息");
        }
        return message;
    }

    private String currentUserId() {
        String userId = UserContext.get() == null ? null : UserContext.get().getUserId();
        if (StringUtils.isBlank(userId)) {
            throw new BusinessException("用户未登录");
        }
        return userId;
    }

    private String resolveChatName(ChatMessage message, String userId) {
        try {
            if (message.getChatType() != null && message.getChatType() == ChatType.GROUP.getType()) {
                Group group = groupService.getById(message.getReceiverId());
                if (group != null) return group.getGroupName();
                return "群聊";
            }
            if (message.getChatType() != null && message.getChatType() == ChatType.BOT.getType()) {
                BotDetailVO bot = botService.getBotDetail(message.getReceiverId());
                if (bot != null) return bot.getName();
                return "AI 对话";
            }
            String peerId = StringUtils.equals(message.getSenderId(), userId)
                    ? message.getReceiverId() : message.getSenderId();
            User user = userService.getById(peerId);
            return user == null ? "单聊" : user.getUsername();
        } catch (Exception e) {
            log.warn("解析收藏会话名称失败 messageId={}", message.getId(), e);
            return "聊天";
        }
    }

    private String resolveSenderName(String senderId, String userId) {
        if (StringUtils.equals(senderId, userId)) return "我";
        try {
            User user = userService.getById(senderId);
            if (user != null) return user.getUsername();
        } catch (Exception ignore) {
            // 机器人消息的 senderId 不在 xg_user 中，走机器人兜底
        }
        try {
            BotDetailVO bot = botService.getBotDetail(senderId);
            if (bot != null) return bot.getName();
        } catch (Exception ignore) {
            // 名称只是展示信息，查不到不影响收藏落库
        }
        return "对方";
    }

    /**
     * 回应变更实时通知：群聊广播给成员，单聊推给双方。离线用户下次打开会话会拉到最新状态。
     */
    private void notifyReaction(ChatMessage message, String userId, String emoji, boolean active) {
        JSONObject payload = new JSONObject();
        payload.put("chatMessageId", message.getId());
        payload.put("chatType", message.getChatType());
        payload.put("receiverId", message.getReceiverId());
        payload.put("userId", userId);
        payload.put("emoji", emoji);
        payload.put("active", active);
        String json = payload.toJSONString();

        Set<String> recipients = new HashSet<>();
        if (message.getChatType() != null && message.getChatType() == ChatType.GROUP.getType()) {
            Set<String> members = groupMemberService.getGroupMembers(message.getReceiverId());
            if (CollectionUtils.isNotEmpty(members)) recipients.addAll(members);
        } else {
            recipients.add(message.getSenderId());
            recipients.add(message.getReceiverId());
        }
        for (String receiverId : recipients) {
            if (StringUtils.isBlank(receiverId)) continue;
            Client client = centerService.onlineUser(receiverId);
            if (client == null) continue;
            MessageResponse response = new MessageResponse();
            response.setSenderId(Sender.SYSTEM.getSender());
            response.setReceiverId(receiverId);
            response.setMessageType(MessageType.CHAT.getType());
            response.setSubType(MessageSubType.MES_REACTION.getType());
            response.setMessage(json);
            response.setCreateTime(String.valueOf(System.currentTimeMillis()));
            try {
                centerService.sendMessage2Client(response, client);
            } catch (Exception e) {
                log.warn("回应实时通知失败 receiverId={} messageId={}", receiverId, message.getId(), e);
            }
        }
    }
}
