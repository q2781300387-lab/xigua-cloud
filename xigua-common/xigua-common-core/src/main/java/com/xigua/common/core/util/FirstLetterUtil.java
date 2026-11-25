package com.xigua.common.core.util;

import com.github.promeg.pinyinhelper.Pinyin;

/**
 * @ClassName FirstLetterUtil
 * @Description
 * @Author wangjinfei
 * @Date 2026/4/15 10:18
 */
public class FirstLetterUtil {

    /**
     * 获取字符串首字母（A-Z），其他返回 #
     */
    public static String getFirstLetter(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "#";
        }

        char c = name.trim().charAt(0);

        // 1️⃣ 中文处理
        if (isChinese(c)) {
            try {
                String pinyin = Pinyin.toPinyin(c);
                if (pinyin != null && !pinyin.isEmpty()) {
                    return String.valueOf(Character.toUpperCase(pinyin.charAt(0)));
                }
            } catch (Exception e) {
                return "#";
            }
        }

        // 2️⃣ 英文处理
        if (Character.isLetter(c)) {
            return String.valueOf(Character.toUpperCase(c));
        }

        // 3️⃣ 数字 / 特殊字符
        return "#";
    }

    /**
     * 判断是否是中文
     */
    private static boolean isChinese(char c) {
        return String.valueOf(c).matches("[\\u4E00-\\u9FA5]");
    }

    /**
     * 是否是有效字母 A-Z
     */
    public static boolean isValidLetter(String letter) {
        return letter != null && letter.matches("[A-Z]");
    }
}
