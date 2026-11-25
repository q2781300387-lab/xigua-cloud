package com.xigua.common.core.util;

/**
 * @ClassName IpUtil
 * @Description
 * @Author wangjinfei
 * @Date 2026/3/17 20:49
 */
public class IpUtil {
    /**
     * IP脱敏
     * @param ip 原始IP
     * @return 脱敏后的IP
     */
    public static String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***.***";
        }
        return ip;
    }

    /**
     * 登录区域脱敏
     * @param region 原始区域信息，格式：上海|上海市
     * @return 脱敏后的区域
     */
    public static String maskRegion(String region) {
        if (region == null || region.isEmpty()) {
            return "unknown";
        }
        // 如果是unknown，直接返回
        if ("unknown".equals(region)) {
            return region;
        }
        // 解析格式：上海|上海市，第二部分用***代替
        String[] parts = region.split("\\|");
        if (parts.length >= 2) {
            String area = parts[0];
            return "0".equals(area) ? "unknown" : area + "|***";
        }
        return region;
    }
}
