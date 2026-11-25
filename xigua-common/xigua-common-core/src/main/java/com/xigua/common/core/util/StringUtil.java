package com.xigua.common.core.util;

import org.apache.commons.lang3.StringUtils;

/**
 * @ClassName StringUtil
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/1 17:31
 */
public class StringUtil {
    public static String maskEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return email;
        }

        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return email;
        }

        // 保留第一个字符和@后面的部分
        String prefix = email.substring(0, 1);
        String suffix = email.substring(atIndex);

        return prefix + "****" + suffix;
    }

    /**
     * IP地址脱敏
     * @author wangjinfei
     * @date 2026/3/10
     * @param ip
     * @return String
     */
    public static String maskIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return ip;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return ip;
        }

        return parts[0] + ".***.***." + parts[3];
    }
}
