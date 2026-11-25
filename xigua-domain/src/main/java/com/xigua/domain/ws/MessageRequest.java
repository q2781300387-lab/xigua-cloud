package com.xigua.domain.ws;

import com.xigua.domain.bo.SenderBO;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName MessageRequest
 * @Description 消息请求对象
 * @Author wangjinfei
 * @Date 2024/12/2 16:28
 */
@Data
public class MessageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 发送人id
     */
    private String senderId;

    /**
     * 接收人id
     */
    private String receiverId;

    /**
     * 消息类型
     * @see com.xigua.domain.enums.MessageType
     */
    private String messageType;

    /**
     * 子类型
     * @see com.xigua.domain.enums.MessageSubType
     */
    private String subType;

    /**
     * 消息文本正文
     */
    private String message;

    /**
     * 聊天类型
     * @see com.xigua.domain.enums.ChatType
     */
    private Integer chatType;

    /**
     * 消息类型
     * 目前只对chat的发送和接收有用
     * @see com.xigua.domain.enums.ContentType
     */
    private Integer contentType;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 聊天消息id
     */
    private String chatMessageId;

    /**
     * 客户端消息id（幂等键，重发时保持不变）
     */
    private String clientMsgId;

    /**
     * 引用的消息id
     */
    private String replyToId;

    /**
     * 引用消息内容快照
     */
    private String replyContent;

    /**
     * @提及的用户id列表（JSON数组字符串）
     */
    private String atUsers;

    /**
     * 呼叫id
     */
    private String callId;

    /**
     * 服务端收到消息的时间
     */
    private Long serverReceiveTime;

    /**
     * 纬度
     */
    private String lat;

    /**
     * 经度
     */
    private String lng;

    /**
     * ip地址
     */
    private String ip;
}
