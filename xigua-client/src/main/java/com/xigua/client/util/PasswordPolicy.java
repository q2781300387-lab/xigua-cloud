package com.xigua.client.util;

import java.util.regex.Pattern;

/**
 * 密码强度规则（注册与重置密码共用一套口径）
 *
 * 规则：
 *  1. 长度 8-20
 *  2. 不能包含空白字符
 *  3. 必须同时包含字母与数字
 *  4. 不能与用户名相同
 *  5. 不能包含 3 个及以上连续重复字符（aaa / 111）
 *
 * @author wangjinfei
 * @date 2026/11/12
 */
public final class PasswordPolicy {

    public static final int MIN_LEN = 8;
    public static final int MAX_LEN = 20;

    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final Pattern LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern REPEAT = Pattern.compile("(.)\\1{2,}");

    private PasswordPolicy() {
    }

    /**
     * 校验密码强度
     *
     * @param raw      明文密码
     * @param username 用户名，注册场景传入（用于「密码不能与用户名相同」）；改密场景传 null
     * @return null 表示通过，否则返回可直接返回给前端的错误文案
     */
    public static String check(String raw, String username) {
        if (raw == null || raw.isEmpty()) {
            return "密码不可以为空";
        }
        int len = raw.length();
        if (len < MIN_LEN) {
            return "密码至少 " + MIN_LEN + " 位";
        }
        if (len > MAX_LEN) {
            return "密码最多 " + MAX_LEN + " 位";
        }
        if (WHITESPACE.matcher(raw).find()) {
            return "密码不能包含空格";
        }
        if (!LETTER.matcher(raw).find()) {
            return "密码必须包含字母";
        }
        if (!DIGIT.matcher(raw).find()) {
            return "密码必须包含数字";
        }
        if (username != null && !username.isEmpty() && raw.equalsIgnoreCase(username)) {
            return "密码不能与用户名相同";
        }
        if (REPEAT.matcher(raw).find()) {
            return "密码不能包含 3 个及以上连续重复的字符";
        }
        return null;
    }
}
