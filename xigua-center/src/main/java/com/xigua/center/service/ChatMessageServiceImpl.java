package com.xigua.center.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xigua.center.mapper.ChatMessageMapper;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.bo.LastMessageContentBO;
import com.xigua.domain.bo.ConversationPeerBO;
import com.xigua.domain.bo.MessageReaderBO;
import com.xigua.domain.bo.SenderBO;
import com.xigua.domain.dto.GetLastMesDTO;
import com.xigua.domain.dto.GetHistoryMes;
import com.xigua.domain.dto.GetCallRecordsDTO;
import com.xigua.domain.entity.Bot;
import com.xigua.domain.entity.ChatMessage;
import com.xigua.domain.entity.Group;
import com.xigua.domain.entity.MessageRead;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.ChatType;
import com.xigua.domain.enums.MessageReadStatus;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.util.BasePage;
import com.xigua.domain.vo.ChatMessageVO;
import com.xigua.domain.vo.CallRecordVO;
import com.xigua.domain.vo.LastMessageVO;
import com.xigua.domain.ws.MessageResponse;
import com.xigua.api.service.BotService;
import com.xigua.api.service.CenterService;
import com.xigua.api.service.ChatMessageService;
import com.xigua.api.service.GroupMemberService;
import com.xigua.api.service.MessageReadService;
import com.xigua.api.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName ChatMessageServiceImpl
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/17 19:19
 */
@Slf4j
@Service
@DubboService
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private CenterService centerService;
    @DubboReference
    private UserService userService;
    @DubboReference
    private BotService botService;
    @DubboReference(check = false, timeout = 3000)
    private GroupMemberService groupMemberService;
    @Autowired
    private MessageReadService messageReadService;
    @Autowired
    private ConversationSettingService conversationSettingService;
    @Autowired
    private com.xigua.common.sequence.sequence.Sequence sequence;

    /**
     * 获取好友最后一条消息
     * @author wangjinfei
     * @date 2025/5/17 19:25
     * @param dto
     * @return List<LastChatVO>
     */
    @Override
    public BasePageVO<LastMessageVO> getLastMes(GetLastMesDTO dto) {
        String userId = UserContext.get().getUserId();
        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        List<LastMessageVO> lastChatList = new ArrayList<>();

        // todo 后续 可以扩展置顶消息

        // 已退群/被移出的群会话不应该继续出现在列表里，否则用户点开只能看到 500
        Set<String> activeGroupIds = loadActiveGroupIds(userId);

        // 会话列表原本只读 Redis：缓存被清空、或消息是直接写库导入的，列表就会整片空白。
        // 以 xg_chat_message 为准补齐缺失的会话，避免「好友和群都在、会话列表却是空的」
        syncLastMesFromDb(userId, activeGroupIds);

        // 从redis查询最后聊天消息
        String lastMesKey = RedisEnum.LAST_MES.getKey() + userId;
        Long totalCount = redisClient.zsetSize(lastMesKey);
        long start = (pageNum - 1) * pageSize;
        long end = start + pageSize - 1;
        Set<Object> lastMessages = redisClient.zsReverseRange(lastMesKey, start, end);

        // 遍历最后聊天消息 （获取内容、在线状态等）
        int filtered = 0;
        for (Object lastMessage : lastMessages) {
            LastMessageVO vo = getLastMes(userId, lastMessage.toString());
            // 成员关系已经失效：顺手清掉 Redis，避免列表和未读数一直残留
            if (activeGroupIds != null && vo != null
                    && vo.getChatType() != null
                    && vo.getChatType() == ChatType.GROUP.getType()
                    && !activeGroupIds.contains(vo.getChatId())) {
                redisClient.zsrem(lastMesKey, lastMessage);
                redisClient.hashDel(RedisEnum.LAST_MES_CONTENT.getKey() + userId, lastMessage.toString());
                filtered++;
                continue;
            }
            lastChatList.add(vo);
        }
        totalCount = Math.max(0, totalCount - filtered);

        // 附带当前用户的置顶 / 草稿，并让置顶会话排在本页最前
        List<String> chatIds = new ArrayList<>();
        for (LastMessageVO vo : lastChatList) {
            if (vo != null && vo.getChatId() != null) chatIds.add(vo.getChatId());
        }
        Map<String, com.xigua.domain.vo.ConversationSettingVO> settings =
                conversationSettingService.batchGet(userId, chatIds);
        for (LastMessageVO vo : lastChatList) {
            if (vo == null) continue;
            com.xigua.domain.vo.ConversationSettingVO setting =
                    settings.get(vo.getChatType() + ":" + vo.getChatId());
            vo.setPinned(setting != null && Boolean.TRUE.equals(setting.getPinned()));
            vo.setDraft(setting == null ? "" : setting.getDraft());
        }
        lastChatList.sort((a, b) -> {
            boolean pa = a != null && Boolean.TRUE.equals(a.getPinned());
            boolean pb = b != null && Boolean.TRUE.equals(b.getPinned());
            if (pa != pb) return pa ? -1 : 1;
            long ta = a == null || a.getUpdateTime() == null ? 0L : a.getUpdateTime();
            long tb = b == null || b.getUpdateTime() == null ? 0L : b.getUpdateTime();
            return Long.compare(tb, ta);
        });

        // 封装分页
        BasePageVO<LastMessageVO> result = new BasePageVO<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setRows(lastChatList);
        result.setTotalPage((totalCount + pageSize - 1) / pageSize);
        result.setTotalCount(totalCount);

        return result;
    }

    /**
     * 当前用户仍在的群。Dubbo 调用失败时返回 null，宁可不过滤，也不能把正常会话误删。
     */
    private Set<String> loadActiveGroupIds(String userId) {
        try {
            return groupMemberService.getGroupIdsByUserId(userId);
        } catch (Exception e) {
            log.warn("查询用户群列表失败，本轮跳过已退群过滤 userId={}", userId, e);
            return null;
        }
    }

    /**
     * 用数据库补齐会话列表缓存。
     * 会话列表原本完全依赖 Redis，缓存被清空、或消息是直接写库导入的，就会整片丢失；
     * 这里以 xg_chat_message 里每个会话的最新一条为准重建，缓存里更新的（刚发出未落库）保留不覆盖。
     * @param userId 当前用户id
     */
    private void syncLastMesFromDb(String userId, Set<String> activeGroupIds) {
        List<ConversationPeerBO> peers = baseMapper.selectLatestPerPeer(userId);
        if (CollectionUtils.isEmpty(peers)) {
            return;
        }
        String lastMesKey = RedisEnum.LAST_MES.getKey() + userId;
        String contentKey = RedisEnum.LAST_MES_CONTENT.getKey() + userId;
        int filled = 0;
        for (ConversationPeerBO peer : peers) {
            if (StringUtils.isEmpty(peer.getPeerId())) {
                continue;
            }
            // 已退出的群不能再从数据库回填进会话列表，否则每次刷新都会重新冒出来
            if (activeGroupIds != null
                    && peer.getChatType() != null
                    && peer.getChatType() == ChatType.GROUP.getType()
                    && !activeGroupIds.contains(peer.getPeerId())) {
                continue;
            }
            long epoch = peer.getCreateTime() == null
                    ? System.currentTimeMillis()
                    : peer.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            // 缓存已有且不比数据库旧，说明是刚发出去还没落库的消息，保留缓存
            Object cachedObj = redisClient.hashGet(contentKey, peer.getPeerId());
            if (cachedObj != null) {
                Long cachedTime = null;
                try {
                    cachedTime = JSONObject.parseObject(cachedObj.toString(), LastMessageVO.class).getUpdateTime();
                } catch (Exception ignore) {
                    // 历史脏数据解析失败，按缺失处理重新写一份
                }
                if (cachedTime != null && cachedTime >= epoch) {
                    continue;
                }
            }

            LastMessageVO vo = new LastMessageVO();
            vo.setChatId(peer.getPeerId());
            vo.setChatType(peer.getChatType());
            vo.setUpdateTime(epoch);
            resolveChatMeta(vo, peer.getPeerId());

            LastMessageContentBO content = new LastMessageContentBO();
            content.setContent(peer.getMessage());
            vo.setLastMessageContent(content);

            redisClient.zsadd(lastMesKey, peer.getPeerId(), epoch);
            redisClient.hashPut(contentKey, peer.getPeerId(), JSONObject.toJSONString(vo));
            filled++;
        }
        if (filled > 0) {
            log.info("会话列表缓存回源补齐 userId={} 补入 {} 个会话", userId, filled);
        }
    }

    /**
     * 根据发送人和接收人获取最后一条消息
     * @author wangjinfei
     * @date 2025/12/14 17:06
     * @param senderId
     * @param receiverId
     * @return LastMessageVO
     */
    @Override
    public LastMessageVO getLastMes(String senderId, String receiverId) {
        LastMessageVO lastMessageVO = new LastMessageVO();
        Long unreadCount = 0L;
        Boolean isOnline = false;

        // 获取最后一条消息
        Object mesObj = redisClient.hashGet(RedisEnum.LAST_MES_CONTENT.getKey() + senderId, receiverId);

        // 获取最后一条消息的内容
        // LastMessageVO只在LastMessageBO字段上只扩展字段，其他字段相同，就直接用LastMessageVO接收
        if(mesObj != null){
            lastMessageVO = JSONObject.parseObject(mesObj.toString(), LastMessageVO.class);
        }else{
            // 当mesObj为空是，查询receiverId的chatType、chatName、avatar等基础信息
            setReceiverDefaultInfo(lastMessageVO, receiverId);
        }

        Integer chatType = lastMessageVO.getChatType();
        if (chatType == null) {
            // 查不到会话主体的老数据会留下 null，直接参与比较就是拆箱空指针
            chatType = ChatType.PRIVATE.getType();
            lastMessageVO.setChatType(chatType);
        }
//        String senderId = lastMessageVO.getChatId();

        // 聊天类型
        if (chatType == ChatType.PRIVATE.getType()) {
            // 好友是否在线
            isOnline = centerService.isOnline(receiverId);
            // 好友未读消息数量
            // 未读计数的 key 只有真正产生过未读时才存在，hincrget 会返回 null，
            // 直接赋给 Long 拆箱就是空指针，整个会话列表接口都会 500
            Long friendUnread = redisClient.hincrget(RedisEnum.FRIEND_UNREAD_COUNT.getKey() + senderId, receiverId);
            unreadCount = friendUnread == null ? 0L : friendUnread;
        }else if(chatType == ChatType.GROUP.getType()){
            isOnline = false;
            // 群聊未读消息数量
            Long groupUnread = redisClient.hincrget(RedisEnum.GROUP_UNREAD_COUNT.getKey() + senderId, receiverId);
            unreadCount = groupUnread == null ? 0L : groupUnread;
        }else if(chatType == ChatType.BOT.getType()){
            isOnline = false;
            unreadCount = 0L;
        }else {
            isOnline = false;
            unreadCount = 0L;
        }
        lastMessageVO.setIsOnline(isOnline);
        lastMessageVO.setUnreadCount(unreadCount);
        return lastMessageVO;
    }

    private void setReceiverDefaultInfo(LastMessageVO lastMessageVO, String receiverId){
        // 当mesObj为空是，查询receiverId的chatType、chatName、avatar等基础信息
        lastMessageVO.setChatId(receiverId);
        lastMessageVO.setUpdateTime(System.currentTimeMillis());
        LastMessageContentBO lastMessageContentBO = new LastMessageContentBO();
        lastMessageContentBO.setContent("");
        lastMessageVO.setLastMessageContent(lastMessageContentBO);
        resolveChatMeta(lastMessageVO, receiverId);
    }

    /**
     * 只补会话主体信息（类型 / 名称 / 头像），不碰最后一条消息内容。
     * 缓存整片失效时逐个回源数据库并回填，否则会话列表会出现「有会话、没名字」。
     * 外部 Dubbo 调用失败只记日志，不能让整个会话列表 500。
     */
    private void resolveChatMeta(LastMessageVO lastMessageVO, String receiverId) {
        if (StringUtils.isEmpty(receiverId)) {
            return;
        }

        // 用户
        User user = null;
        String userCache = redisClient.get(RedisEnum.USER.getKey() + receiverId);
        if (StringUtils.isNotEmpty(userCache)) {
            user = JSONObject.parseObject(userCache, User.class);
        }
        if (user == null) {
            try {
                user = userService.getById(receiverId);
            } catch (Exception e) {
                log.warn("会话主体回源用户失败 receiverId={}", receiverId, e);
            }
            if (user != null) {
                redisClient.set(RedisEnum.USER.getKey() + receiverId, JSONObject.toJSONString(user));
            }
        }
        if (user != null) {
            lastMessageVO.setChatType(ChatType.PRIVATE.getType());
            lastMessageVO.setChatName(user.getUsername());
            lastMessageVO.setAvatar(user.getAvatar());
            return;
        }

        // 群组
        Group group = null;
        String groupCache = redisClient.get(RedisEnum.GROUP.getKey() + receiverId);
        if (StringUtils.isNotEmpty(groupCache)) {
            group = JSONObject.parseObject(groupCache, Group.class);
        }
        if (group == null) {
            group = baseMapper.selectGroupMeta(receiverId);
            if (group != null) {
                redisClient.set(RedisEnum.GROUP.getKey() + receiverId, JSONObject.toJSONString(group));
            }
        }
        if (group != null) {
            lastMessageVO.setChatType(ChatType.GROUP.getType());
            lastMessageVO.setChatName(group.getGroupName());
            lastMessageVO.setAvatar(group.getGroupAvatar());
            return;
        }

        // 查询机器人
        String robotCache = redisClient.get(RedisEnum.BOT.getKey() + receiverId);
        Bot robot = StringUtils.isNotEmpty(robotCache) ? JSONObject.parseObject(robotCache, Bot.class) : null;
        if (robot == null) {
            // 缓存丢失时机器人会话会被当成普通私聊，列表里没有名字也点不进 AI 分支，回查一次数据库并回填
            robot = botService.getById(receiverId);
            if (robot != null) {
                redisClient.set(RedisEnum.BOT.getKey() + receiverId, JSONObject.toJSONString(robot));
            }
        }
        if (robot != null) {
            lastMessageVO.setChatType(ChatType.BOT.getType());
            lastMessageVO.setChatName(robot.getName());
            lastMessageVO.setAvatar(robot.getAvatar());
            return;
        }

        // 会话主体已不存在（好友删除 / 群解散 / 机器人删除）。名字不能留 null：
        // 前端列表直接取首字符渲染，null 会让整批会话一起渲染失败
        lastMessageVO.setChatName("未知会话");
    }

    /**
     * 添加好友最后一条消息到redis
     * @author wangjinfei
     * @date 2025/12/15 15:51
     * @param senderId
     * @param receiverId
     * @param chatType
     * @return LastMessageVO
     */
    @Override
    public LastMessageVO addLastMes2Redis(String senderId, String receiverId, Integer chatType) {
        LastMessageVO lastMessageVO = getLastMes(senderId, receiverId);
        long timestamp = System.currentTimeMillis();
        Long updateTime = lastMessageVO.getUpdateTime();
        if(updateTime != null){
            timestamp = updateTime;
        }

        // 同步redis 最后消息
        redisClient.zsadd(RedisEnum.LAST_MES.getKey() + senderId, receiverId, timestamp);
        // 同步redis 最后消息内容
        redisClient.hashPut(RedisEnum.LAST_MES_CONTENT.getKey() + senderId, receiverId, JSONObject.toJSONString(lastMessageVO));
        return lastMessageVO;
    }

    /**
     * 获取置顶用户
     * fixme 暂时不考虑置顶消息
     * @author wangjinfei
     * @date 2025/5/20 22:23
     * @param topUserId
     * @param lastChatList
    */
//    public void getTopUser(String topUserId, List<LastMessageVO> lastChatList){
//        String userId = UserContext.get().getUserId();
//        User topUser = userService.getById(topUserId);
//        if (topUser == null) {
//            throw new BusinessException("置顶用户不存在");
//        }
//
//        // 置顶用户
//        // todo 可以优化成从redis获取
//        LastMessageVO topLastMessageVO = new LastMessageVO();
//        topLastMessageVO.setUserId(topUserId);
//        topLastMessageVO.setUsername(topUser.getUsername());
//        topLastMessageVO.setAvatar(topUser.getAvatar());
//
//        // 获取置顶用户最后一条消息
//        Object mesObj = redisClient.hashGet(RedisEnum.LAST_MES.getKey() + userId, topUserId);
//        if (mesObj != null) {
//            JSONObject lastFriendMes = JSONObject.parseObject(mesObj.toString());
//            topLastMessageVO.setLastMessage(lastFriendMes.getString("message"));
//        }else{
//            topLastMessageVO.setLastMessage("");
//        }
//
//        Boolean online = centerService.isOnline(topUserId);
//        topLastMessageVO.setIsOnline(online);
//        lastChatList.add(topLastMessageVO);
//    }

    /**
     * 分页获取好友历史消息
     * @author wangjinfei
     * @date 2025/5/25 10:16
     * @param dto
     * @return BasePageVO<ChatMessageVO>
     */
    @Override
    public BasePageVO<ChatMessageVO> getHistoryMes(GetHistoryMes dto) {
        String userId = UserContext.get().getUserId();
        String receiverId = dto.getReceiverId();
        Integer chatType = dto.getChatType();
        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        Page<ChatMessageVO> page = new Page<>(pageNum, pageSize);
        List<ChatMessageVO> chatMesList = new ArrayList<>();
        // 好友聊天
        if (chatType == ChatType.PRIVATE.getType()) {
            // 查询历史消息（我发给好友和好友发给我的消息，按时间倒序查询）
            chatMesList = baseMapper.getPrivateChatHistoryMes(page, userId, receiverId);
        }else if(chatType == ChatType.GROUP.getType()){
            // 群历史消息必须校验当前登录人是否在群内，否则任何账号都能按 groupId 读别人的群聊记录
            groupMemberService.getGroupRole(receiverId, userId);
            chatMesList = baseMapper.getGroupChatHistoryMes(page, userId, receiverId);
        }else if(chatType == ChatType.BOT.getType()){
            chatMesList = baseMapper.getBotChatHistoryMes(page, userId, receiverId);
        }else{
            throw new BusinessException("聊天类型不存在");
        }

        // 返回空分页
        if(CollectionUtils.isEmpty(chatMesList)){
            return BasePage.emptyResult(page);
        }

        // 群聊
        if(chatType == ChatType.GROUP.getType()){
            // 遍历发送人信息
            for (ChatMessageVO chatMessageVO : chatMesList) {
                SenderBO senderBo = new SenderBO();
                String senderId = chatMessageVO.getSenderId();
                String userCache = redisClient.get(RedisEnum.USER.getKey() + senderId);
                if(StringUtils.isNotEmpty(userCache)){
                    User user = JSONObject.parseObject(userCache, User.class);
                    senderBo.setUsername(user.getUsername());
                    senderBo.setAvatar(user.getAvatar());
                }
                chatMessageVO.setSender(senderBo);
            }
            // 已读回填：刷新页面 / 重进会话后，已读人数不再从 0 开始
            fillGroupReadInfo(chatMesList);
        }

        // 反转消息列表 页面显示最新的消息在最下面
        Collections.reverse(chatMesList);
        BasePageVO<ChatMessageVO> result = BasePage.getResult(page, chatMesList);

        return result;
    }

    /**
     * 独立通话记录中心：分页读取单聊视频通话卡片。
     * 呼出由主叫落一条记录，双方看到同一条；这里统一按“我 vs 对端”的方向展示。
     */
    @Override
    public BasePageVO<CallRecordVO> getCallRecords(GetCallRecordsDTO dto) {
        String userId = UserContext.get().getUserId();
        if (StringUtils.isEmpty(userId)) {
            throw new BusinessException("用户未登录");
        }
        Integer pageNum = dto.getPageNum() == null ? 1 : dto.getPageNum();
        Integer pageSize = dto.getPageSize() == null ? 20 : Math.min(dto.getPageSize(), 50);
        Page<CallRecordVO> page = new Page<>(pageNum, pageSize);
        String keyword = StringUtils.isEmpty(dto.getKeyword()) ? null : dto.getKeyword().trim();
        List<CallRecordVO> rows = baseMapper.getCallRecords(page, userId,
                Boolean.TRUE.equals(dto.getMissedOnly()), keyword,
                dto.getStartTime(), dto.getEndTime());
        if (CollectionUtils.isEmpty(rows)) {
            return BasePage.emptyResult(page);
        }
        for (CallRecordVO row : rows) {
            String outcome = "completed";
            Integer duration = 0;
            try {
                JSONObject body = JSONObject.parseObject(row.getMessage());
                if (body != null) {
                    if (StringUtils.isNotEmpty(body.getString("outcome"))) {
                        outcome = body.getString("outcome");
                    }
                    if (body.getInteger("duration") != null) {
                        duration = body.getInteger("duration");
                    }
                }
            } catch (Exception ignored) {
                // 旧数据消息体异常时按已接通兜底，不能让整页记录打不开
            }
            row.setOutcome(outcome);
            row.setDuration(duration);
            row.setMissed("incoming".equals(row.getDirection())
                    && ("rejected".equals(outcome) || "timeout".equals(outcome) || "busy".equals(outcome)));
            row.setMessage(null);
        }
        return BasePage.getResult(page, rows);
    }

    @Override
    public int countUnreadCallRecords() {
        String userId = currentCallUserId();
        return baseMapper.countUnreadCallRecords(userId);
    }

    @Override
    public void markCallRecordsRead() {
        baseMapper.markCallRecordsRead(currentCallUserId());
    }

    @Override
    public void deleteCallRecord(String messageId) {
        if (StringUtils.isEmpty(messageId)) {
            throw new BusinessException("缺少通话记录id");
        }
        baseMapper.deleteCallRecordForUser(currentCallUserId(), messageId.trim());
    }

    @Override
    public void clearCallRecords() {
        baseMapper.clearCallRecordsForUser(currentCallUserId());
    }

    private String currentCallUserId() {
        String userId = UserContext.get().getUserId();
        if (StringUtils.isEmpty(userId)) {
            throw new BusinessException("用户未登录");
        }
        return userId;
    }

    /**
     * 给群聊历史消息补已读人数与名单
     * 数据源是 xg_message_read（is_read=1），一次批量查出来再按 messageId 聚合，避免 N+1
     * @author wangjinfei
     * @date 2026/11/12
     * @param chatMesList 群聊历史消息
     */
    private void fillGroupReadInfo(List<ChatMessageVO> chatMesList){
        Set<String> messageIds = chatMesList.stream()
                .map(ChatMessageVO::getChatMessageId)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        if(CollectionUtils.isEmpty(messageIds)){
            return;
        }
        List<MessageRead> readList = messageReadService.list(
                new LambdaQueryWrapper<MessageRead>()
                        .in(MessageRead::getMessageId, messageIds)
                        .eq(MessageRead::getIsRead, MessageReadStatus.READ.getType())
        );
        Map<String, Map<String, MessageReaderBO>> grouped = new HashMap<>();
        for (MessageRead row : readList) {
            if(StringUtils.isEmpty(row.getReceiverId())){
                continue;
            }
            Map<String, MessageReaderBO> readers = grouped.computeIfAbsent(row.getMessageId(), k -> new LinkedHashMap<>());
            // 同一个人对同一条消息重复上报时只算一次
            if(readers.containsKey(row.getReceiverId())){
                continue;
            }
            MessageReaderBO reader = new MessageReaderBO();
            reader.setId(row.getReceiverId());
            reader.setName(resolveUserName(row.getReceiverId()));
            readers.put(row.getReceiverId(), reader);
        }
        for (ChatMessageVO vo : chatMesList) {
            Map<String, MessageReaderBO> readers = grouped.get(vo.getChatMessageId());
            if(readers == null || readers.isEmpty()){
                vo.setReadCount(0);
                vo.setReaders(new ArrayList<>());
                continue;
            }
            vo.setReaders(new ArrayList<>(readers.values()));
            vo.setReadCount(readers.size());
        }
    }

    /**
     * 从用户缓存里取昵称，取不到返回空串（前端会兜底显示）
     * @author wangjinfei
     * @date 2026/11/12
     * @param userId 用户id
     * @return String
     */
    private String resolveUserName(String userId){
        if(StringUtils.isEmpty(userId)){
            return "";
        }
        try {
            String userCache = redisClient.get(RedisEnum.USER.getKey() + userId);
            if(StringUtils.isEmpty(userCache)){
                return "";
            }
            User user = JSONObject.parseObject(userCache, User.class);
            return user != null && user.getUsername() != null ? user.getUsername() : "";
        } catch (Exception e) {
            log.warn("[group-read] 解析已读人昵称失败 userId={}", userId, e);
            return "";
        }
    }

    /**
     * 获取好友最后一条消息
     * @author wangjinfei
     * @date 2025/8/9 12:11
     * @param senderId
     * @param receiverId
     * @return ChatMessage
     */
    @Override
    public ChatMessage getLastMessage(String senderId, String receiverId) {
        return baseMapper.getLastMessage(senderId, receiverId);
    }

    @Override
    public ChatMessage getLastMes(String senderId, String receiverId, Integer chatType) {
        ChatMessage latMes = null;
        if (chatType == ChatType.PRIVATE.getType()) {
            latMes = baseMapper.getPrivateLastMes(senderId, receiverId);
        }else if(chatType == ChatType.GROUP.getType()){
            latMes = baseMapper.getGroupLastMes(receiverId);
        }
        return latMes;
    }

    @Override
    public Long countTodayMessages() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        return this.count(new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getDelFlag, 0)
            .ge(ChatMessage::getCreateTime, todayStart)
        );
    }

    @Override
    public Long countYesterdayMessages() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime yesterdayStart = LocalDateTime.of(yesterday, LocalTime.MIN);
        LocalDateTime yesterdayEnd = LocalDateTime.of(yesterday, LocalTime.MAX);
        return this.count(new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getDelFlag, 0)
            .ge(ChatMessage::getCreateTime, yesterdayStart)
            .le(ChatMessage::getCreateTime, yesterdayEnd)
        );
    }

    @Override
    public List<String> getActiveGroupIds() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return this.list(new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getChatType, 2)
            .eq(ChatMessage::getDelFlag, 0)
            .ge(ChatMessage::getCreateTime, sevenDaysAgo)
            .select(ChatMessage::getReceiverId)
        ).stream()
        .map(ChatMessage::getReceiverId)
        .distinct()
        .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<String> getYesterdayActiveGroupIds() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime eightDaysAgo = yesterday.atStartOfDay().minusDays(7);
        LocalDateTime yesterdayEnd = LocalDateTime.of(yesterday, LocalTime.MAX);
        return this.list(new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getChatType, 2)
            .eq(ChatMessage::getDelFlag, 0)
            .ge(ChatMessage::getCreateTime, eightDaysAgo)
            .le(ChatMessage::getCreateTime, yesterdayEnd)
            .select(ChatMessage::getReceiverId)
        ).stream()
        .map(ChatMessage::getReceiverId)
        .distinct()
        .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<com.xigua.domain.vo.admin.MessageTrendItemVO> getMessageTrend24Hours() {
        List<com.xigua.domain.vo.admin.MessageTrendItemVO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 查询最近24小时的所有消息
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        List<ChatMessage> messages = this.list(new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getDelFlag, 0)
            .ge(ChatMessage::getCreateTime, twentyFourHoursAgo)
            .le(ChatMessage::getCreateTime, now)
        );

        // 初始化24小时的数据结构（从当前小时往前推24小时）
        for (int i = 23; i >= 0; i--) {
            LocalDateTime hourTime = now.minusHours(i);
            String hourStr = String.format("%02d:00", hourTime.getHour());
            result.add(com.xigua.domain.vo.admin.MessageTrendItemVO.builder()
                .timePoint(hourStr)
                .messageCount(0)
                .build());
        }

        // 统计每个小时的消息数量
        for (ChatMessage message : messages) {
            if (message.getCreateTime() != null) {
                int messageHour = message.getCreateTime().getHour();
                int currentHour = now.getHour();

                // 计算消息时间点在结果列表中的索引
                int index;
                if (messageHour <= currentHour) {
                    // 消息在今天的某个小时
                    index = currentHour - messageHour;
                } else {
                    // 消息在昨天的某个小时
                    index = 24 - (messageHour - currentHour);
                }

                if (index >= 0 && index < 24) {
                    com.xigua.domain.vo.admin.MessageTrendItemVO item = result.get(index);
                    item.setMessageCount(item.getMessageCount() + 1);
                }
            }
        }

        return result;
    }

    @Override
    public Long getTotalMessages() {
        return this.count(new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getDelFlag, 0)
        );
    }

    @Override
    public Long getTotalMessagesByUser(String userId) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(userId)) {
            return 0L;
        }
        return this.count(new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getSenderId, userId)
            .eq(ChatMessage::getDelFlag, 0)
        );
    }

    @Override
    public Long countTodayMessagesByUser(String userId) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(userId)) {
            return 0L;
        }
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        return this.count(new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getSenderId, userId)
            .eq(ChatMessage::getDelFlag, 0)
            .ge(ChatMessage::getCreateTime, todayStart)
        );
    }

    @Override
    public Long countYesterdayMessagesByUser(String userId) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(userId)) {
            return 0L;
        }
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime yesterdayStart = LocalDateTime.of(yesterday, LocalTime.MIN);
        LocalDateTime yesterdayEnd = LocalDateTime.of(yesterday, LocalTime.MAX);
        return this.count(new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getSenderId, userId)
            .eq(ChatMessage::getDelFlag, 0)
            .ge(ChatMessage::getCreateTime, yesterdayStart)
            .le(ChatMessage::getCreateTime, yesterdayEnd)
        );
    }

    @Override
    public Long countGroupMessages(String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            return 0L;
        }
        return this.count(new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getChatType, 2)
            .eq(ChatMessage::getReceiverId, groupId)
            .eq(ChatMessage::getDelFlag, 0)
        );
    }

    @Override
    public Map<String, LocalDateTime> getGroupMemberLastMessageTimes(String groupId, List<String> senderIds) {
        if (StringUtils.isEmpty(groupId) || CollectionUtils.isEmpty(senderIds)) {
            return java.util.Map.of();
        }

        List<com.xigua.domain.vo.admin.GroupMemberLastMessageVO> rows = baseMapper.getGroupMemberLastMessageTimes(groupId, senderIds);
        if (CollectionUtils.isEmpty(rows)) {
            return java.util.Map.of();
        }

        return rows.stream().collect(java.util.stream.Collectors.toMap(
            com.xigua.domain.vo.admin.GroupMemberLastMessageVO::getSenderId,
            com.xigua.domain.vo.admin.GroupMemberLastMessageVO::getLastMessageTime
        ));
    }

    /**
     * 写入一条群系统消息
     * messageType=6 为系统提示类型，前端按该类型渲染居中灰条；落库保证刷新后仍在
     * @author doubao
     * @date 2026/11/12
     * @param groupId 群组id
     * @param content 展示文案
     * @param operatorId 触发操作的用户id
     * @return String 消息id
     */
    @Override
    public String saveGroupSystemMessage(String groupId, String content, String operatorId) {
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(content)) {
            return null;
        }

        ChatMessage message = new ChatMessage();
        String messageId = sequence.nextNo();
        message.setId(messageId);
        message.setSenderId(StringUtils.isEmpty(operatorId) ? "0" : operatorId);
        message.setReceiverId(groupId);
        message.setChatType(2);
        message.setMessageType(6);
        message.setMessage(content);
        message.setDelFlag(0);
        message.setWithdrawn(0);
        message.setCreateTime(LocalDateTime.now());
        this.save(message);

        log.info("--------->>>>>> 群 {} 写入系统消息 {}：{}", groupId, messageId, content);
        return messageId;
    }
    /**
     * 编辑自己发送的文本消息（5 分钟内），并广播给会话参与者
     */
    @Override
    public void editMessage(String messageId, String content) {
        String userId = UserContext.get().getUserId();
        if (StringUtils.isBlank(messageId)) {
            throw new BusinessException("消息id不能为空");
        }
        if (StringUtils.isBlank(content)) {
            throw new BusinessException("消息内容不能为空");
        }
        if (content.length() > 20000) {
            throw new BusinessException("消息内容过长");
        }
        ChatMessage message = getById(messageId);
        if (message == null || Integer.valueOf(1).equals(message.getDelFlag())) {
            throw new BusinessException("消息不存在");
        }
        if (!StringUtils.equals(userId, message.getSenderId())) {
            throw new BusinessException("只能编辑自己发送的消息");
        }
        if (Integer.valueOf(1).equals(message.getWithdrawn())) {
            throw new BusinessException("已撤回的消息不能编辑");
        }
        if (!Integer.valueOf(1).equals(message.getMessageType())) {
            throw new BusinessException("仅文本消息支持编辑");
        }
        if (message.getCreateTime() != null && message.getCreateTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BusinessException("消息发送超过 5 分钟，不能再编辑");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean updated = update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getId, messageId)
                .set(ChatMessage::getMessage, content)
                .set(ChatMessage::getEdited, 1)
                .set(ChatMessage::getEditTime, now));
        if (!updated) {
            throw new BusinessException("编辑失败，请稍后重试");
        }

        MessageResponse response = new MessageResponse();
        response.setSenderId(message.getSenderId());
        response.setReceiverId(message.getReceiverId());
        response.setChatType(message.getChatType());
        response.setChatMessageId(message.getId());
        response.setMessage(content);
        response.setMessageType(com.xigua.domain.enums.MessageType.CHAT.getType());
        response.setSubType("mes_edit");
        response.setCreateTime(now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (ChatType.GROUP.getType().equals(message.getChatType())) {
            Set<String> members = groupMemberService.getGroupMembers(message.getReceiverId());
            if (CollectionUtils.isNotEmpty(members)) {
                for (String memberId : members) {
                    centerService.sendMessage2User(response, memberId);
                }
            }
        } else {
            centerService.sendMessage2User(response, message.getSenderId());
            centerService.sendMessage2User(response, message.getReceiverId());
        }
        log.info("--------->>>>>> 用户 {} 编辑消息 {} 成功", userId, messageId);
    }
}
