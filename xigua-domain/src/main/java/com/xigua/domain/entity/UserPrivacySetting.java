package com.xigua.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户隐私与通知偏好。一个用户一条记录，缺省时按默认值处理。
 */
@Data
@TableName("xg_user_privacy_setting")
@Schema(title = "用户隐私设置")
public class UserPrivacySetting implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private String id;

    private String userId;

    /** 通知是否展示消息内容 */
    private Integer notificationPreview;

    /** 新消息提示音 */
    private Integer notificationSound;

    /** 是否向他人展示在线状态 */
    private Integer showOnlineStatus;

    /** 是否发送已读回执 */
    private Integer showReadReceipt;

    /** 是否允许通过手机号搜索到我 */
    private Integer allowPhoneSearch;

    /** 加好友是否需要验证（1 需要，0 不需要） */
    private Integer friendVerify;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
