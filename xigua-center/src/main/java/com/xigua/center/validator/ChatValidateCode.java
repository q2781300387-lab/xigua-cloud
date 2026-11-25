package com.xigua.center.validator;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public enum ChatValidateCode {
    SUCCESS(0, "成功"),
    NOT_FRIEND(10001, "你们不是好友关系，无法发送消息"),
    GROUP_MUTED(10002, "全员禁言中，仅群主和管理员可发言"),
    NOT_GROUP_MEMBER(10003, "用户未加入群聊"),
    MESSAGE_EMPTY(10004, "消息内容不能为空"),
    MESSAGE_TOO_LONG(10005, "消息内容过长"),
    ACCOUNT_MUTED(10006, "账号已被禁言，暂时无法发送消息"),
    ACCOUNT_BANNED(10007, "账号已被封禁，无法发送消息"),
    GROUP_MEMBER_MUTED(10008, "你已被群管理员禁言，暂时无法发言"),
    ;
    private final Integer code;
    private final String msg;

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
