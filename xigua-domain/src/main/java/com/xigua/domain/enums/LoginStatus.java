package com.xigua.domain.enums;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public enum LoginStatus {
    SUCCESS(1,"成功"),
    FAIL(0,"失败"),
    ;

    final Integer type;

    final String desc;

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }


    public static LoginStatus getChatType(Integer type) {
        for (LoginStatus loginStatus : LoginStatus.values()) {
            if(loginStatus.type.equals(type)){
                return loginStatus;
            }
        }
        return null;
    }
}
