package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName GroupDetailVO
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/7/30 19:29
 */
@Data
public class GroupDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 群组id
     */
    @Schema(description = "群组id")
    private String groupId;

    /**
     * 群组名称
     */
    @Schema(description = "群组名称")
    private String groupName;

    /**
     * 群组头像
     */
    @Schema(description = "群组头像")
    private String groupAvatar;

    /**
     * 群组描述
     */
    @Schema(description = "群组描述")
    private String description;

    /**
     * 群公告
     */
    @Schema(description = "群公告")
    private String announcement;

    /**
     * 公告更新时间
     */
    @Schema(description = "公告更新时间")
    private String announcementTime;

    /**
     * 公告发布者名称
     */
    @Schema(description = "公告发布者名称")
    private String announcementEditor;

    /**
     * 全员禁言（0：关闭；1：开启）
     */
    @Schema(description = "全员禁言（0：关闭；1：开启）")
    private Integer muteAll;

    /**
     * 群主用户id
     */
    @Schema(description = "群主用户id")
    private String ownerId;

    /**
     * 当前成员数
     */
    @Schema(description = "当前成员数")
    private Integer currentMember;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private String createTime;

    /**
     * 群主
     */
    @Schema(description = "群主")
    private String groupOwner;

    /**
     * 我的角色
     */
    @Schema(description = "我的角色")
    private String roleName;

    /**
     * 我的角色类型（1：群主；2：群管理；3：普通成员）
     * 与文案解耦，前端判断权限时使用该字段而不是 roleName
     */
    @Schema(description = "我的角色类型（1：群主；2：群管理；3：普通成员）")
    private Integer role;

    /**
     * 加入时间
     */
    @Schema(description = "加入时间")
    private String joinTime;

    /**
     * 成员列表（含角色，群详情页展示与操作使用）
     */
    @Schema(description = "成员列表")
    private java.util.List<GroupMemberVO> members;
}
