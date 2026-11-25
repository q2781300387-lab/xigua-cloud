package com.xigua.domain.vo;

import com.xigua.domain.bo.LastMessageContentBO;
import com.xigua.domain.enums.ChatType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName LastChatVO
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/5/17 19:23
 */
@Data
@Schema(description = "最后一条消息")
public class LastMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 聊天id
     * 单聊：好友id
     * 群聊：群id
    */
    @Schema(description = "聊天id")
    private String chatId;

    /**
     * 聊天类型
     * @see ChatType
     */
    @Schema(description = "聊天类型（1：单聊；2：群聊；3：机器人）")
    private Integer chatType;

    /**
     * 聊天名称
     */
    @Schema(description = "聊天名称")
    private String chatName;

    /**
     * 头像
     */
     @Schema(description = "头像")
    private String avatar;

    /**
     * 最后一条消息内容
     */
     @Schema(description = "最后一条消息内容")
    private LastMessageContentBO lastMessageContent;

    /**
     * 未读消息数量
     */
     @Schema(description = "未读消息数量")
    private Long unreadCount;

    /**
     * 是否在线
     */
     @Schema(description = "是否在线（true：在线；false：不在线）")
    private Boolean isOnline;

    /**
     * 最后更新时间
     */
     @Schema(description = "最后更新时间")
    private Long updateTime;

    /** 是否置顶 */
    private Boolean pinned;

    /** 会话草稿 */
    private String draft;

    // todo 后续可能扩展的字段
//    private Boolean isMuted;       // 是否静音
//    private Boolean isStickTop;    // 是否置顶
//    private Long stickTopTime;     // 置顶时间(用于排序)
//    private Boolean isAtMe;        // 是否有@我的消息(群聊)
}
