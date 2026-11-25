package com.xigua.center.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @ClassName PushSubscription
 * @Description Web Push 订阅（center 侧只读，用于离线消息兜底推送）
 * @Author xigua
 * @Date 2026/11/12 19:35
 */
@Data
@TableName("xg_push_subscription")
public class PushSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long userId;

    private String endpoint;

    private String p256dh;

    private String auth;

    private String ua;

    @TableLogic
    private Integer delFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
