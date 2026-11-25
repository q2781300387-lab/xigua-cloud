package com.xigua.center.chatMessage;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigua.api.service.CenterService;
import com.xigua.api.service.GroupMemberService;
import com.xigua.api.service.MessageReadService;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.connect.Client;
import com.xigua.domain.entity.MessageRead;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.ChatType;
import com.xigua.domain.enums.MessageReadStatus;
import com.xigua.domain.enums.MessageSubType;
import com.xigua.domain.enums.MessageType;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.enums.Sender;
import com.xigua.domain.ws.MessageRequest;
import com.xigua.domain.ws.MessageResponse;
import com.xigua.common.core.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @ClassName GroupReadChatMessageService
 * @Description 群聊已读。提交未读的人把「谁读了哪几条」推给群里其他成员，
 *              只有消息发送者会认领并累计已读人数（前端按 messageId 去重累加）
 * @Author wangjinfei
 * @Date 2025/8/24 11:42
 */
@Slf4j
@Component
public class GroupReadChatMessageService extends AbstractReadChatMessageService {
    @Autowired
    private MessageReadService messageReadService;
    @Autowired
    private CenterService centerService;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private Sequence sequence;
    @DubboReference
    private GroupMemberService groupMemberService;


    @Override
    protected ChatType getChatType() {
        return ChatType.GROUP;
    }

    /**
     * 通知发送人消息已读。
     * 群成员提交已读时，一次可能带一批消息 id，这里合并成一帧推给除读者外的在线成员：
     * 发送者收到后按 messageId 认领，累计「谁读了」，前端据此显示「N 人已读」并展开名单。
     * 只推在线用户，与 typing 一致：离线不落库、不补发。
     * @author wangjinfei
     * @date 2025/8/24 11:32
     * @param messageRequest
     */
    @Override
    public void notifySenderOfMessageRead(MessageRequest messageRequest) {
        String readerId = messageRequest.getSenderId();
        String groupId = messageRequest.getReceiverId();
        String raw = messageRequest.getMessage();
        if (StringUtils.isEmpty(readerId) || StringUtils.isEmpty(groupId) || StringUtils.isEmpty(raw)) {
            return;
        }
        List<String> chatMessageIds;
        try {
            chatMessageIds = com.alibaba.fastjson2.JSONArray.parseArray(raw, String.class);
        } catch (Exception e) {
            log.warn("--------->>>>> 群聊已读解析消息id失败，groupId={}，raw={}", groupId, raw, e);
            return;
        }
        if (CollectionUtils.isEmpty(chatMessageIds)) {
            return;
        }

        Set<String> groupMemberIds = groupMemberService.getGroupMembers(groupId);
        if (CollectionUtils.isEmpty(groupMemberIds)) {
            return;
        }
        // 只有群成员才认作「读了这条群消息」，非成员上报一律丢弃，避免已读人数被刷高
        if (!groupMemberIds.contains(readerId)) {
            log.warn("--------->>>>> 群聊已读丢弃非群成员上报，groupId={}，readerId={}", groupId, readerId);
            return;
        }

        // 发消息时若没给该成员建未读记录（成员快照缺口），已读就无处可写、历史回填会少人，
        // 这里按「读者确实读了」补一条已读记录，保证实时推送与刷新后的人数一致
        ensureReadRecord(chatMessageIds, readerId, groupId);

        String readerName = resolveUserName(readerId);
        // 可读人数 = 群成员去掉读者自己
        int memberCount = groupMemberIds.size() - 1;
        String idsJson = JSONObject.toJSONString(chatMessageIds);

        for (String memberId : groupMemberIds) {
            if (memberId.equals(readerId)) {
                continue;
            }
            Client client = centerService.onlineUser(memberId);
            if (client == null) {
                continue;
            }
            try {
                MessageResponse messageResponse = new MessageResponse();
                messageResponse.setSenderId(Sender.SYSTEM.getSender());
                messageResponse.setReceiverId(memberId);
                messageResponse.setMessageType(MessageType.CHAT.getType());
                messageResponse.setSubType(MessageSubType.MES_READ.getType());

                JSONObject payload = new JSONObject();
                payload.put("readChatMessageIds", idsJson);
                // 谁读的、叫什么、群里应读多少人，供发送者累计与展示名单
                payload.put("readerId", readerId);
                payload.put("readerName", readerName);
                payload.put("groupMemberCount", memberCount);

                messageResponse.setMessage(payload.toJSONString());
                messageResponse.setCreateTime(DateUtil.formatDateTime(LocalDateTime.now(), DateUtil.DATE_TIME_FORMATTER));
                centerService.sendMessage2Client(messageResponse, client);
            } catch (Exception e) {
                log.warn("--------->>>>> 群聊已读推送失败，memberId={}，groupId={}", memberId, groupId, e);
            }
        }
    }

    /**
     * 给缺失已读记录的读者补一条（消息发送时的成员快照可能与当前成员不一致）
     * @param chatMessageIds 消息id列表
     * @param readerId 读者id
     * @param groupId 群id
     */
    private void ensureReadRecord(List<String> chatMessageIds, String readerId, String groupId) {
        try {
            List<MessageRead> existing = messageReadService.list(
                    new LambdaQueryWrapper<MessageRead>()
                            .in(MessageRead::getMessageId, chatMessageIds)
                            .eq(MessageRead::getReceiverId, readerId)
            );
            Set<String> recorded = existing.stream()
                    .map(MessageRead::getMessageId)
                    .collect(Collectors.toSet());
            List<MessageRead> addList = new ArrayList<>();
            for (String messageId : chatMessageIds) {
                if (recorded.contains(messageId)) {
                    continue;
                }
                MessageRead record = new MessageRead();
                record.setId(sequence.nextNo());
                record.setMessageId(messageId);
                // 群聊消息的发送者可反查，但这里只需要「谁读了」，senderId 留空不影响统计
                record.setReceiverId(readerId);
                record.setIsRead(MessageReadStatus.READ.getType());
                record.setReadTime(LocalDateTime.now());
                record.setCreateBy(readerId);
                record.setDelFlag(0);
                addList.add(record);
            }
            if (!addList.isEmpty()) {
                messageReadService.saveBatchPlus(addList);
            }
        } catch (Exception e) {
            log.warn("--------->>>>> 群聊已读补记录失败，groupId={}，readerId={}", groupId, readerId, e);
        }
    }

    /**
     * 从用户缓存取昵称，取不到返回空串，前端兜底成「有人」
     * @param userId 用户 id
     */
    private String resolveUserName(String userId) {
        try {
            String userCache = redisClient.get(RedisEnum.USER.getKey() + userId);
            if (StringUtils.isNotEmpty(userCache)) {
                User user = JSONObject.parseObject(userCache, User.class);
                if (user != null && StringUtils.isNotEmpty(user.getUsername())) {
                    return user.getUsername();
                }
            }
        } catch (Exception e) {
            log.warn("--------->>>>> 群聊已读取昵称失败，userId={}", userId, e);
        }
        return "";
    }

    /**
     * 清空未读消息数量
     * @author wangjinfei
     * @date 2025/8/24 11:35
     * @param messageRequest
     */
    @Override
    public void clearUnreadCount(MessageRequest messageRequest) {
        String senderId = messageRequest.getSenderId();
        String receiverId = messageRequest.getReceiverId();
        String groupUnreadCountKey = RedisEnum.GROUP_UNREAD_COUNT.getKey() + senderId;

        // 获取当前未读数（扣减前）
        Object unreadObj = redisClient.hashGet(groupUnreadCountKey, receiverId);
        Long currentUnread = unreadObj instanceof Number ? ((Number) unreadObj).longValue() : null;

        // 清零群聊未读数
        redisClient.hashPut(groupUnreadCountKey, receiverId, 0);

        // 扣减总未读数
        if (currentUnread != null && currentUnread > 0) {
            String chatUnreadCountKey = RedisEnum.CHAT_UNREAD_COUNT.getKey() + senderId;
            redisClient.incrby(chatUnreadCountKey, -currentUnread);
        }
    }
}
