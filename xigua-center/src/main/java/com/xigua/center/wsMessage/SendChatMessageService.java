package com.xigua.center.wsMessage;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.SearchSyncService;
import com.xigua.center.ai.GroupAIMessageService;
import com.xigua.center.chatMessage.AbstractSendChatMessageService;
import com.xigua.center.chatMessage.SendChatMessageServiceFactory;
import com.xigua.center.mapper.ChatMessageMapper;
import com.xigua.center.validator.ChatValidateCode;
import com.xigua.center.validator.MessagePermissionValidator;
import com.xigua.center.validator.ValidatorFactory;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.thread.ThreadMgr;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.connect.Client;
import com.xigua.domain.vo.ChatMessageSyncVO;
import com.xigua.domain.ws.MessageRequest;
import com.xigua.domain.enums.*;
import com.xigua.api.service.CenterService;
import com.xigua.api.service.GroupMemberService;
import com.xigua.api.service.GroupService;
import com.xigua.domain.entity.Group;
import com.xigua.domain.entity.User;
import com.xigua.domain.entity.ChatMessage;
import com.xigua.domain.ws.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @ClassName MesSendHandler
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/6/2 17:44
 */
@Slf4j
@Component
public class SendChatMessageService extends AbstractMessageService {
    private static final DateTimeFormatter CREATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private Sequence sequence;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private CenterService centerService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ValidatorFactory validatorFactory;
    @Autowired
    private GroupAIMessageService groupAIMessageService;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @DubboReference
    private SearchSyncService searchSyncService;
    @DubboReference
    private GroupService groupService;
    @DubboReference
    private GroupMemberService groupMemberService;

    @Override
    public String getMessageName() {
        return MessageSubType.MES_SEND.getDesc();
    }

    @Override
    public String getMessageType() {
        return MessageType.CHAT.getType();
    }

    @Override
    public String getMessageSubType() {
        return MessageSubType.MES_SEND.getType();
    }

    @Override
    public void handleMessage(MessageRequest messageRequest) {
        // 空消息 / 超长内容在入库前拦截，避免直接构造 WS 帧打爆表或拖垮广播
        if (StringUtils.isBlank(messageRequest.getMessage())) {
            validateFail(ChatValidateCode.MESSAGE_EMPTY, messageRequest);
            return;
        }
        if (messageRequest.getMessage().length() > 20000) {
            validateFail(ChatValidateCode.MESSAGE_TOO_LONG, messageRequest);
            return;
        }
        // 账号级封禁/禁言优先拦截，任何聊天类型都不能绕过
        ChatValidateCode accountCode = accountRestrictionCode(messageRequest.getSenderId());
        if (!ChatValidateCode.SUCCESS.equals(accountCode)) {
            log.info("--------->>>>>> 账号 {} 发送消息被拦截：{}", messageRequest.getSenderId(), accountCode.getMsg());
            validateFail(accountCode, messageRequest);
            return;
        }
        // 群消息必须先校验发送人确实是群成员，且要在入库前完成：
        // 否则任何登录用户都能指定任意 groupId 往别人的群里发消息
        if (!isGroupMemberForSender(messageRequest)) {
            log.info("--------->>>>>> 非群成员 {} 试图向群 {} 发送消息，已拦截",
                    messageRequest.getSenderId(), messageRequest.getReceiverId());
            validateFail(ChatValidateCode.NOT_GROUP_MEMBER, messageRequest);
            return;
        }
        // 单人禁言：群主/管理员不受单人禁言限制；查询失败时放行，避免群服务抖动导致全群发不出消息
        if (isGroupMemberMutedForSender(messageRequest)) {
            log.info("--------->>>>>> 群 {} 成员 {} 处于单人禁言中，已拦截",
                    messageRequest.getReceiverId(), messageRequest.getSenderId());
            validateFail(ChatValidateCode.GROUP_MEMBER_MUTED, messageRequest);
            return;
        }
        // 全员禁言必须放在入库之前：被拦截的消息不落库、不计未读、不触发 AI 小助手
        if (isGroupMutedForSender(messageRequest)) {
            log.info("--------->>>>>> 群 {} 全员禁言中，已拦截成员 {} 的消息",
                    messageRequest.getReceiverId(), messageRequest.getSenderId());
            validateFail(ChatValidateCode.GROUP_MUTED, messageRequest);
            return;
        }

        // 私聊好友关系等权限必须在入库/ACK 之前校验，否则非好友消息会先落库再报错，
        // 既产生脏数据，也让客户端误以为发送成功。
        ChatValidateCode validateCode = validateMessage(messageRequest);
        if (!validateCode.equals(ChatValidateCode.SUCCESS)) {
            validateFail(validateCode, messageRequest);
            return;
        }

        // 客户端重发：同一 clientMsgId 直接回原消息id，不重复落库、不重复推送
        if (StringUtils.isNotEmpty(messageRequest.getClientMsgId())) {
            ChatMessage existing = chatMessageMapper.getByClientMsgId(
                    messageRequest.getSenderId(), messageRequest.getClientMsgId());
            if (existing != null) {
                messageRequest.setChatMessageId(existing.getId());
                if (centerService.isOnline(messageRequest.getSenderId())) {
                    pushSendAck(messageRequest);
                }
                return;
            }
        }

        String chatMessageId = sequence.nextNo();
        String receiverId = messageRequest.getReceiverId();
        messageRequest.setChatMessageId(chatMessageId);

        // 定义事务
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        // 开启事务
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            // db处理 mysql持久化
            messageDbHandle(messageRequest);

            // ack处理 (回传消息id给发送人、超时回滚)
            ackHandle(messageRequest, status);

            // 执行到这里没有异常 提交事务（保证mysql入库）
            transactionManager.commit(status);
        } catch (DuplicateKeyException duplicate) {
            // 同一 clientMsgId 并发重发：唯一索引兜底，回滚本次后回查首条并回 ACK
            if (!status.isCompleted()) {
                transactionManager.rollback(status);
            }
            ChatMessage existing = StringUtils.isNotEmpty(messageRequest.getClientMsgId())
                    ? chatMessageMapper.getByClientMsgId(messageRequest.getSenderId(), messageRequest.getClientMsgId())
                    : null;
            if (existing == null) {
                throw duplicate;
            }
            messageRequest.setChatMessageId(existing.getId());
            if (centerService.isOnline(messageRequest.getSenderId())) {
                pushSendAck(messageRequest);
            }
            return;
        }

        // todo 下面所有逻辑处理都可以放到mq里做排队异步处理，提高系统吞吐量

        // 校验消息权限（已在入库前完成，此处不再重复校验）

        // 最后一条消息  (消息列表)
        lastMessage(messageRequest);

        // 聊天消息发送到接收人
        chatMessage2Receiver(messageRequest);

        // 未读消息数量
        unreadMessageCount(messageRequest);

        // 消息未读到消息已读
        unread2Read(messageRequest);

        // 群聊 AI 小助手：@AI / @小瓜 / @老学究 / /ai 提问，机器人异步回复，不影响主流程
        try {
            groupAIMessageService.tryAnswer(messageRequest);
        } catch (Exception e) {
            log.error("--------->>>>>>> 群聊AI小助手触发失败，忽略", e);
        }
    }

    /**
     * ack处理
     * 回传消息id给发送人
     * id在服务端生成，客户端应该第一时间拿到消息id，也为后续已读做铺垫
     * 超时回滚
     * @author wangjinfei
     * @date 2025/6/6 21:02
     * @param messageRequest
    */
    private void ackHandle(MessageRequest messageRequest, TransactionStatus status){
        String senderId = messageRequest.getSenderId();
        // 获取发送人所在的节点信息
        Client client = centerService.onlineUser(senderId);
        if(client == null){
            return;
        }

        // 发送ack之前，判断是否需要发送ack
        sendAckBefore(messageRequest, status);

        pushSendAck(messageRequest);
    }

    /**
     * 组装并发送发送成功 ACK。
     * 保留原有“消息内容 -> 消息id”的 map，兼容旧客户端；
     * 同时回传 clientMsgId / chatMessageId，新客户端按 clientMsgId 精确匹配。
     */
    private void pushSendAck(MessageRequest messageRequest) {
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setSenderId(Sender.SYSTEM.getSender());
        messageResponse.setReceiverId(messageRequest.getSenderId());
        messageResponse.setMessageType(MessageType.CHAT.getType());
        messageResponse.setSubType(MessageSubType.MES_SEND_ACK.getType());
        String json = JSONObject.of(messageRequest.getMessage(), messageRequest.getChatMessageId()).toJSONString();
        messageResponse.setMessage(json);
        messageResponse.setChatMessageId(messageRequest.getChatMessageId());
        messageResponse.setClientMsgId(messageRequest.getClientMsgId());
        messageResponse.setCreateTime(messageRequest.getCreateTime());
        centerService.sendMessage2User(messageResponse, messageRequest.getSenderId());
    }

    /**
     * 发送ack之前，判断是否需要发送ack
     * @author wangjinfei
     * @date 2025/8/12 21:29
    */
    private void sendAckBefore(MessageRequest messageRequest, TransactionStatus status){
        if(messageRequest.getServerReceiveTime() == null){
            transactionManager.rollback(status);
            throw new BusinessException("--------->>>>>>> 服务端收到消息的时间不能为空");
        }

        // 模拟超时，可以验证客户端1500毫秒未收到ack导致的消息发送失败
//        mockTimeout(status);

        // 前端发送消息后1500毫秒没有接收到服务端的ack，认为发送失败
        long sendAckBeforeTime = System.currentTimeMillis();
        if(sendAckBeforeTime - messageRequest.getServerReceiveTime() > 1400){ // 服务端要判断1400毫秒
            transactionManager.rollback(status);
            // 系统报错，不往下进行任何处理
            throw new BusinessException("--------->>>>>>> 服务端收到消息的时间与当前时间差不能超过1400毫秒");
        }
    }

    /**
     * 模拟超时
     * @author wangjinfei
     * @date 2025/8/13 8:10
     * @param status
    */
    private void mockTimeout(TransactionStatus status){
        try{
            // 假标志 控制是否阻塞
            String flag = redisClient.get("flag");
            if(StringUtils.isEmpty(flag)){
                Thread.sleep(1800);
            }
        }catch (Exception e){
            transactionManager.rollback(status);
            e.printStackTrace();
        }
    }

    /**
     * 校验消息权限
     * @author wangjinfei
     * @date 2025/9/13 18:12
     * @param messageRequest
     * @return Boolean
    */
    private ChatValidateCode validateMessage(MessageRequest messageRequest){
        Integer chatType = messageRequest.getChatType();
        if(chatType == null){
            throw new BusinessException("--------->>>>>>> 聊天类型不能为空");
        }

        ChatType chatTypeE = ChatType.getChatType(chatType);
        if(chatTypeE == null){
            throw new BusinessException("--------->>>>>>> 聊天类型不存在");
        }

        // 群聊 或 ai对话 不校验
        if(chatTypeE == ChatType.GROUP || chatTypeE == ChatType.BOT){
            return ChatValidateCode.SUCCESS;
        }

        // 获取校验器
        MessagePermissionValidator validator = validatorFactory.getValidator(chatTypeE);
        if(validator == null){
            throw new BusinessException("--------->>>>>>> 校验器不存在");
        }

        // 校验权限
        ChatValidateCode chatValidateCode = validator.validatePermission(messageRequest);

        return chatValidateCode;
    }

    /**
     * 群消息发送人必须是群成员；非群聊消息直接放行。
     * 查询失败按「不是成员」处理，宁可拒绝也不让越权消息落库。
     */
    private boolean isGroupMemberForSender(MessageRequest messageRequest) {
        if (messageRequest == null
                || !ChatType.GROUP.getType().equals(messageRequest.getChatType())
                || StringUtils.isEmpty(messageRequest.getReceiverId())
                || StringUtils.isEmpty(messageRequest.getSenderId())) {
            return true;
        }
        try {
            groupMemberService.getGroupRole(messageRequest.getReceiverId(), messageRequest.getSenderId());
            return true;
        } catch (Exception e) {
            log.warn("--------->>>>>> 群成员校验失败，拒绝发送：groupId={}，senderId={}",
                    messageRequest.getReceiverId(), messageRequest.getSenderId(), e);
            return false;
        }
    }

    /**
     * 判断发送人是否处于群内单人禁言。
     * 群主与管理员不受单人禁言限制；查询异常时放行，避免依赖故障造成全群静默。
     */
    private boolean isGroupMemberMutedForSender(MessageRequest messageRequest) {
        if (messageRequest == null
                || !ChatType.GROUP.getType().equals(messageRequest.getChatType())
                || StringUtils.isEmpty(messageRequest.getReceiverId())
                || StringUtils.isEmpty(messageRequest.getSenderId())) {
            return false;
        }
        try {
            Integer role = groupMemberService.getGroupRole(messageRequest.getReceiverId(), messageRequest.getSenderId());
            if (GroupRole.OWNER.getType().equals(role) || GroupRole.GROUP_ADMIN.getType().equals(role)) {
                return false;
            }
            LocalDateTime muteUntil = groupMemberService.getMuteUntil(
                    messageRequest.getReceiverId(), messageRequest.getSenderId());
            return muteUntil != null && muteUntil.isAfter(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("--------->>>>>> 单人禁言校验失败，放行本次消息：groupId={}，senderId={}",
                    messageRequest.getReceiverId(), messageRequest.getSenderId(), e);
            return false;
        }
    }

    /**
     * 判断当前发送人是否被全员禁言拦住
     * 规则：群 muteAll=1 时只有群主、管理员可以发言；查询失败时放行，避免群服务抖动导致全群发不出消息
     * @param messageRequest 消息请求
     * @return true 表示应拦截
     */
    private boolean isGroupMutedForSender(MessageRequest messageRequest) {
        if (messageRequest == null
                || !ChatType.GROUP.getType().equals(messageRequest.getChatType())
                || StringUtils.isEmpty(messageRequest.getReceiverId())
                || StringUtils.isEmpty(messageRequest.getSenderId())) {
            return false;
        }
        try {
            String groupId = messageRequest.getReceiverId();
            List<Group> groups = groupService.getListByIds(List.of(groupId));
            if (groups == null || groups.isEmpty() || groups.get(0) == null) {
                return false;
            }
            Group group = groups.get(0);
            if (group.getMuteAll() == null || group.getMuteAll() != 1) {
                return false;
            }
            Integer role = groupMemberService.getGroupRole(groupId, messageRequest.getSenderId());
            return role == null
                    || (!GroupRole.OWNER.getType().equals(role) && !GroupRole.GROUP_ADMIN.getType().equals(role));
        } catch (Exception e) {
            log.error("--------->>>>>> 全员禁言校验失败，放行本次消息：groupId={}，senderId={}",
                    messageRequest.getReceiverId(), messageRequest.getSenderId(), e);
            return false;
        }
    }

    /**
     * 校验失败处理
     * @author wangjinfei
     * @date 2025/9/13 18:23
     * @param chatValidateCode
    */
    private void validateFail(ChatValidateCode chatValidateCode, MessageRequest messageRequest){
        // todo 这个方法的处理写死了，应该抽出一层

        if(chatValidateCode == null){
            return;
        }
        // 获取发送人所在的节点信息
        Client client = centerService.onlineUser(messageRequest.getSenderId());
        if(client == null){
            // 发送人不在线，直接返回，不做后续处理
            return;
        }

        // 实时推送消息，发送到发送人所在节点
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setSenderId(Sender.SYSTEM.getSender());
        messageResponse.setReceiverId(messageRequest.getSenderId());
        messageResponse.setMessageType(MessageType.CHAT.getType());
        messageResponse.setSubType(MessageSubType.SYSTEM_MES.getType());
        messageResponse.setMessage(chatValidateCode.getMsg());
        messageResponse.setCreateTime(String.valueOf(System.currentTimeMillis()));
        centerService.sendMessage2Client(messageResponse, client);
    }

    /**
     * 聊天消息发送到接收人
     * @author wangjinfei
     * @date 2025/6/3 20:55
     * @param messageRequest
    */
    private void chatMessage2Receiver(MessageRequest messageRequest){
        ChatType chatTypeE = ChatType.getChatType(messageRequest.getChatType());
        // 调用不同的实现类
        AbstractSendChatMessageService chatMessageService = SendChatMessageServiceFactory.getSendService(chatTypeE.getName());
        chatMessageService.chatMessage2Receiver(messageRequest);
    }

    /**
     * 消息未读到消息已读
     * @author wangjinfei
     * @date 2025/6/3 20:56
     * @param messageRequest
    */
    private void unread2Read(MessageRequest messageRequest){
        ChatType chatTypeE = ChatType.getChatType(messageRequest.getChatType());
        // 调用不同的实现类
        AbstractSendChatMessageService chatMessageService = SendChatMessageServiceFactory.getSendService(chatTypeE.getName());
        chatMessageService.unread2Read(messageRequest);
    }

    /**
     * db处理 mysql持久化
     * @author wangjinfei
     * @date 2025/5/20 22:02
     * @param messageRequest
     */
    private void messageDbHandle(MessageRequest messageRequest){
        Integer chatType = messageRequest.getChatType();
        ChatType chatTypeE = ChatType.getChatType(chatType);

        // 根据不同聊天类型，调用不同的实现类
        AbstractSendChatMessageService chatMessageService = SendChatMessageServiceFactory.getSendService(chatTypeE.getName());

        // 保存聊天消息
        chatMessageService.saveChatMessage(messageRequest);

        // 保存消息未读
        chatMessageService.saveMessageUnread(messageRequest);

        // 异步写入es，供搜索功能使用；失败不影响消息发送主流程
        indexChatMessage2Es(messageRequest);
    }

    /**
     * 异步同步聊天消息到 es
     * @author wangjinfei
     * @date 2026/9/29
     * @param messageRequest
    */
    private void indexChatMessage2Es(MessageRequest messageRequest){
        ThreadMgr.getExecutor().execute(() -> {
            try {
                ChatMessageSyncVO vo = new ChatMessageSyncVO();
                vo.setId(messageRequest.getChatMessageId());
                vo.setSenderId(messageRequest.getSenderId());
                vo.setReceiverId(messageRequest.getReceiverId());
                vo.setMessage(messageRequest.getMessage());
                vo.setChatType(messageRequest.getChatType());
                vo.setMessageType(messageRequest.getContentType());
                vo.setCreateTime(LocalDateTime.now().format(CREATE_TIME_FORMATTER));

                searchSyncService.indexChatMessage(vo);
            } catch (Exception e) {
                log.error("--------->>>>>>> 聊天消息同步到es失败，chatMessageId={}", messageRequest.getChatMessageId(), e);
            }
        });
    }


    /**
     * 最后一条消息  (消息列表)
     * 最后消息好友列表 zset
     * 好友最后消息 hash
     * @author wangjinfei
     * @date 2025/6/9 20:04
     * @param messageRequest
    */
    private void lastMessage(MessageRequest messageRequest){
        ChatType chatTypeE = ChatType.getChatType(messageRequest.getChatType());
        // 调用不同的实现类
        AbstractSendChatMessageService chatMessageService = SendChatMessageServiceFactory.getSendService(chatTypeE.getName());
        chatMessageService.lastMessage(messageRequest);
    }

    /**
     * 未读消息数量
     * @author wangjinfei
     * @date 2025/6/9 20:44
     * @param messageRequest
    */
    private void unreadMessageCount(MessageRequest messageRequest){
        ChatType chatTypeE = ChatType.getChatType(messageRequest.getChatType());
        // 调用不同的实现类
        AbstractSendChatMessageService chatMessageService = SendChatMessageServiceFactory.getSendService(chatTypeE.getName());
        chatMessageService.unreadMessageCount(messageRequest);
    }

    /**
     * 账号级封禁/禁言检查：缓存缺失时放行，避免 Redis 抖动把正常用户全部误拦。
     */
    private ChatValidateCode accountRestrictionCode(String senderId) {
        String userCache = redisClient.get(RedisEnum.USER.getKey() + senderId);
        if (StringUtils.isEmpty(userCache)) {
            return ChatValidateCode.SUCCESS;
        }
        User user = JSONObject.parseObject(userCache, User.class);
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            return ChatValidateCode.SUCCESS;
        }
        if (Integer.valueOf(2).equals(user.getStatus())) {
            return ChatValidateCode.ACCOUNT_BANNED;
        }
        if (Integer.valueOf(1).equals(user.getStatus())) {
            LocalDateTime muteUntil = user.getMuteUntil();
            if (muteUntil == null || muteUntil.isAfter(LocalDateTime.now())) {
                return ChatValidateCode.ACCOUNT_MUTED;
            }
        }
        return ChatValidateCode.SUCCESS;
    }
}
