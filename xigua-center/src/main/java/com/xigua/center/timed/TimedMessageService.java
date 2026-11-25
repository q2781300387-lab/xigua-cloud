package com.xigua.center.timed;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xigua.api.service.GroupMemberService;
import com.xigua.center.wsMessage.SendChatMessageService;
import com.xigua.center.mapper.TimedMessageMapper;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.entity.TimedMessage;
import com.xigua.domain.enums.ChatType;
import com.xigua.domain.enums.ContentType;
import com.xigua.domain.enums.MessageSubType;
import com.xigua.domain.enums.MessageType;
import com.xigua.domain.ws.MessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @ClassName TimedMessageService
 * @Description 定时消息：落库 + 服务端调度投递。页面关闭、换设备都不影响发送。
 */
@Slf4j
@Service
public class TimedMessageService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TimedMessageMapper timedMessageMapper;

    @Autowired
    private Sequence sequence;

    @Autowired
    private SendChatMessageService sendChatMessageService;

    @DubboReference(check = false, timeout = 3000)
    private GroupMemberService groupMemberService;

    /**
     * 预约一条定时消息
     */
    public TimedMessage create(String senderId, String receiverId, Integer chatType,
                               String content, String sendAtText) {
        if (StringUtils.isBlank(senderId) || StringUtils.isBlank(receiverId)) {
            throw new BusinessException("发送人或接收人不能为空");
        }
        if (StringUtils.isBlank(content)) {
            throw new BusinessException("定时消息内容不能为空");
        }
        if (content.length() > 2000) {
            throw new BusinessException("定时消息内容不能超过 2000 字");
        }
        if (chatType == null || chatType < 1 || chatType > 3) {
            throw new BusinessException("聊天类型不合法");
        }
        LocalDateTime sendAt;
        try {
            sendAt = LocalDateTime.parse(sendAtText.trim(), FORMATTER);
        } catch (Exception e) {
            throw new BusinessException("发送时间格式不正确");
        }
        if (sendAt.isBefore(LocalDateTime.now().plusSeconds(10))) {
            throw new BusinessException("发送时间必须晚于当前时间");
        }
        // 定时群消息同样要校验发送人是否在群内，避免借预约绕开实时发送的成员校验
        if (chatType == ChatType.GROUP.getType()) {
            groupMemberService.getGroupRole(receiverId, senderId);
        }

        TimedMessage timedMessage = new TimedMessage();
        timedMessage.setId(sequence.nextNo());
        timedMessage.setSenderId(senderId);
        timedMessage.setReceiverId(receiverId);
        timedMessage.setChatType(chatType);
        timedMessage.setContent(content.trim());
        timedMessage.setSendAt(sendAt);
        timedMessage.setStatus(0);
        timedMessage.setCreateTime(LocalDateTime.now());
        timedMessageMapper.insert(timedMessage);
        return timedMessage;
    }

    /**
     * 查询某人待发送的定时消息（不含已过期未处理的，避免界面展示僵尸任务）
     */
    public List<TimedMessage> listPending(String senderId) {
        if (StringUtils.isBlank(senderId)) {
            throw new BusinessException("发送人不能为空");
        }
        LambdaQueryWrapper<TimedMessage> query = new LambdaQueryWrapper<>();
        query.eq(TimedMessage::getSenderId, senderId)
                .eq(TimedMessage::getStatus, 0)
                .gt(TimedMessage::getSendAt, LocalDateTime.now().minusSeconds(30))
                .orderByAsc(TimedMessage::getSendAt);
        return timedMessageMapper.selectList(query);
    }

    /**
     * 取消定时消息（仅本人、仅待发状态可取消）
     */
    public boolean cancel(String id, String senderId) {
        if (StringUtils.isBlank(id) || StringUtils.isBlank(senderId)) {
            throw new BusinessException("参数不完整");
        }
        LambdaUpdateWrapper<TimedMessage> update = new LambdaUpdateWrapper<>();
        update.eq(TimedMessage::getId, id)
                .eq(TimedMessage::getSenderId, senderId)
                .eq(TimedMessage::getStatus, 0)
                .set(TimedMessage::getStatus, 3)
                .set(TimedMessage::getUpdateTime, LocalDateTime.now());
        return timedMessageMapper.update(null, update) > 0;
    }

    /**
     * 每 10 秒扫描一次到期任务，走真实消息链路投递
     */
    @Scheduled(fixedDelay = 10000)
    public void dispatchDue() {
        LambdaQueryWrapper<TimedMessage> query = new LambdaQueryWrapper<>();
        query.eq(TimedMessage::getStatus, 0)
                .le(TimedMessage::getSendAt, LocalDateTime.now())
                .orderByAsc(TimedMessage::getSendAt)
                .last("limit 20");
        List<TimedMessage> due = timedMessageMapper.selectList(query);
        if (due.isEmpty()) {
            return;
        }
        for (TimedMessage timedMessage : due) {
            try {
                MessageRequest request = new MessageRequest();
                request.setSenderId(timedMessage.getSenderId());
                request.setReceiverId(timedMessage.getReceiverId());
                request.setChatType(timedMessage.getChatType());
                request.setMessage(timedMessage.getContent());
                request.setMessageType(MessageType.CHAT.getType());
                request.setSubType(MessageSubType.MES_SEND.getType());
                request.setContentType(ContentType.TEXT.getValue());
                request.setAtUsers("[]");
                request.setReplyToId("");
                request.setReplyContent("");
                request.setCreateTime(LocalDateTime.now().format(FORMATTER));
                // 服务端内部投递没有前端 ack 环节，需补齐"服务端收到时间"以通过 handleMessage 的 ack 前置校验
                request.setServerReceiveTime(System.currentTimeMillis());
                sendChatMessageService.handleMessage(request);
                timedMessage.setStatus(1);
                log.info("定时消息已发送 id={} sender={} receiver={}",
                        timedMessage.getId(), timedMessage.getSenderId(), timedMessage.getReceiverId());
            } catch (RpcException e) {
                // 依赖服务（群成员/在线状态）短暂不可用时不要判失败，保留待发状态由下一轮重试
                log.warn("定时消息投递依赖服务不可用，稍后重试 id={} reason={}",
                        timedMessage.getId(), e.getMessage());
                continue;
            } catch (Exception e) {
                timedMessage.setStatus(2);
                log.error("定时消息发送失败 id={}", timedMessage.getId(), e);
            }
            timedMessage.setUpdateTime(LocalDateTime.now());
            LambdaUpdateWrapper<TimedMessage> update = new LambdaUpdateWrapper<>();
            update.eq(TimedMessage::getId, timedMessage.getId())
                    .set(TimedMessage::getStatus, timedMessage.getStatus())
                    .set(TimedMessage::getUpdateTime, timedMessage.getUpdateTime());
            int rows = timedMessageMapper.update(null, update);
            if (rows != 1) {
                log.warn("定时消息状态回写异常 id={} status={} rows={}",
                        timedMessage.getId(), timedMessage.getStatus(), rows);
            }
        }
        log.info("定时消息调度完成 count={}", due.size());
    }
}
