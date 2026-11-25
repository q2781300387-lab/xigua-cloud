package com.xigua.domain.enums;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public enum Platform {
    CLIENT_WEB(1,"客户端web"),
    ADMIN_WEB(2,"管理端web"),
    APP(3,"app"),
    MINI(4,"mini")
    ;

    final Integer type;

    final String desc;

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }


    public static Platform getChatType(Integer type) {
        for (Platform platform : Platform.values()) {
            if(platform.type.equals(type)){
                return platform;
            }
        }
        return null;
    }
}
