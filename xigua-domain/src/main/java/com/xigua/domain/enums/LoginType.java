package com.xigua.domain.enums;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public enum LoginType {
    LOG_OUT(0,"登出"),
    PASSWORD(1,"密码"),
    ;

    final Integer type;

    final String desc;

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }


    public static LoginType getChatType(Integer type) {
        for (LoginType loginType : LoginType.values()) {
            if(loginType.type.equals(type)){
                return loginType;
            }
        }
        return null;
    }
}
