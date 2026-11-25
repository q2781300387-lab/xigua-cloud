package com.xigua.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @ClassName MenuType
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/22 21:20
 */
@ToString
@Getter
@AllArgsConstructor
public enum MenuType {
    /**
     * 目录
     */
    DIRECTORY(1, "目录"),

    /**
     * 菜单
     */
    MENU(2, "菜单"),

    /**
     * 按钮
     */
    BUTTON(3, "按钮");

    private final Integer value;
    private final String desc;

    /**
     * 根据值获取枚举
     */
    public static MenuType getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (MenuType type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否是目录
     */
    public static boolean isDirectory(Integer type) {
        return DIRECTORY.getValue().equals(type);
    }

    /**
     * 判断是否是菜单
     */
    public static boolean isMenu(Integer type) {
        return MENU.getValue().equals(type);
    }

    /**
     * 判断是否是按钮
     */
    public static boolean isButton(Integer type) {
        return BUTTON.getValue().equals(type);
    }
}
