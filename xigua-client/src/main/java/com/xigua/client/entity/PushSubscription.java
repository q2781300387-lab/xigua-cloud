package com.xigua.client.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName PushSubscription
 * @Description Web Push 订阅（浏览器维度，同一用户可有多条）
 * @Author xigua
 * @Date 2026/11/12 19:20
 */
@Data
@TableName("xg_push_subscription")
public class PushSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 推送端点（由浏览器颁发，指向厂商推送服务）
     */
    private String endpoint;

    /**
     * 客户端公钥（P-256，base64url）
     */
    private String p256dh;

    /**
     * 客户端认证密钥（base64url）
     */
    private String auth;

    /**
     * 浏览器 UA
     */
    private String ua;

    /**
     * 是否删除（0未删 1已删）
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
