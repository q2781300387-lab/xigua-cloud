package com.xigua.domain.vo;

import com.xigua.domain.bo.MessageReaderBO;
import com.xigua.domain.bo.SenderBO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName MessageBaseDTO
 * @Description
 * @Author wangjinfei
 * @Date 2024/12/2 16:28
 */
@Data
public class ChatMessageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 聊天消息id
     */
    private String chatMessageId;

    /**
     * 发送人id
     */
    private String senderId;

    /**
     * 发送人
     */
    private SenderBO sender;
    /**
     * 接收人id
     */
    private String receiverId;

    /**
     * 聊天类型
     */
    private Integer chatType;

    /**
     * 消息类型（1文本 2图片 3语音 4视频 5文件）
     */
    private Integer messageType;

    /**
     * 消息文本正文
     */
    private String message;

    /**
     * 是否已读（0：未读；1：已读）
     */
    private Boolean isRead;

    /**
     * 是否撤回（0：未撤回；1：已撤回）
     */
    private Integer withdrawn;

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
     * 是否编辑过（0：未编辑；1：已编辑）
     */
    private Integer edited;

    /**
     * 最后编辑时间
     */
    private String editTime;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 群聊已读人数（0 表示还没人读；仅群聊返回）
     */
    private Integer readCount;

    /**
     * 群聊已读人名单（带 id 与昵称；仅群聊返回，最多取前若干个）
     */
    private List<MessageReaderBO> readers;
}
