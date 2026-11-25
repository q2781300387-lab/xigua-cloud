package com.xigua.common.core.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

/**
 * YAUAA User-Agent 解析工具类
 * 用于解析 User-Agent 字符串，提取设备信息、操作系统、浏览器等
 *
 * @author Claude
 * @date 2026/03/20
 */
@Slf4j
public class UserAgentUtil {

    /**
     * UserAgentAnalyzer 实例（单例，线程安全）
     * 由 Spring 容器注入，或懒加载初始化
     */
    private static volatile UserAgentAnalyzer userAgentAnalyzer;

    /**
     * 设置 UserAgentAnalyzer 实例（由 Spring 配置类调用）
     */
    public static void setAnalyzer(UserAgentAnalyzer analyzer) {
        userAgentAnalyzer = analyzer;
        log.info("YAUAA UserAgentAnalyzer 已注入到工具类");
    }

    /**
     * 获取 UserAgentAnalyzer 实例
     */
    private static UserAgentAnalyzer getAnalyzer() {
        return userAgentAnalyzer;
    }

    /**
     * 解析 User-Agent 字符串
     *
     * @param userAgentString User-Agent 字符串
     * @return 解析后的设备信息
     */
    public static UserAgentInfo parse(String userAgentString) {
        if (userAgentString == null || userAgentString.isEmpty()) {
            return null;
        }

        try {
            UserAgent userAgent = getAnalyzer().parse(userAgentString);
            return convertToUserAgentInfo(userAgent);
        } catch (Exception e) {
            log.error("解析 User-Agent 失败: {}", userAgentString, e);
            return null;
        }
    }

    /**
     * 转换为自定义的 UserAgentInfo 对象
     */
    private static UserAgentInfo convertToUserAgentInfo(UserAgent userAgent) {
        UserAgentInfo info = new UserAgentInfo();

        // 设备类型
        info.setDeviceClass(userAgent.getValue(UserAgent.DEVICE_CLASS));
        info.setDeviceName(userAgent.getValue(UserAgent.DEVICE_NAME));
        info.setDeviceBrand(userAgent.getValue(UserAgent.DEVICE_BRAND));

        // 操作系统
        info.setOperatingSystemClass(userAgent.getValue(UserAgent.OPERATING_SYSTEM_CLASS));
        info.setOperatingSystemName(userAgent.getValue(UserAgent.OPERATING_SYSTEM_NAME));
        info.setOperatingSystemVersion(userAgent.getValue(UserAgent.OPERATING_SYSTEM_VERSION));
        info.setOperatingSystemVersionMajor(userAgent.getValue(UserAgent.OPERATING_SYSTEM_VERSION_MAJOR));
        info.setOperatingSystemNameVersion(userAgent.getValue(UserAgent.OPERATING_SYSTEM_NAME_VERSION));

        // 浏览器/应用
        info.setAgentClass(userAgent.getValue(UserAgent.AGENT_CLASS));
        info.setAgentName(userAgent.getValue(UserAgent.AGENT_NAME));
        info.setAgentVersion(userAgent.getValue(UserAgent.AGENT_VERSION));
        info.setAgentVersionMajor(userAgent.getValue(UserAgent.AGENT_VERSION_MAJOR));
        info.setAgentNameVersion(userAgent.getValue(UserAgent.AGENT_NAME_VERSION));

        // 布局引擎
        info.setLayoutEngineClass(userAgent.getValue(UserAgent.LAYOUT_ENGINE_CLASS));
        info.setLayoutEngineName(userAgent.getValue(UserAgent.LAYOUT_ENGINE_NAME));
        info.setLayoutEngineVersion(userAgent.getValue(UserAgent.LAYOUT_ENGINE_VERSION));

        return info;
    }

    /**
     * 判断是否为移动设备
     *
     * @param userAgentString User-Agent 字符串
     * @return true: 移动设备; false: 非移动设备
     */
    public static boolean isMobile(String userAgentString) {
        UserAgentInfo info = parse(userAgentString);
        if (info == null) {
            return false;
        }

        String deviceClass = info.getDeviceClass();
        return "Phone".equals(deviceClass) || "Tablet".equals(deviceClass) || "Mobile".equals(deviceClass);
    }

    /**
     * 判断是否为桌面设备
     *
     * @param userAgentString User-Agent 字符串
     * @return true: 桌面设备; false: 非桌面设备
     */
    public static boolean isDesktop(String userAgentString) {
        UserAgentInfo info = parse(userAgentString);
        if (info == null) {
            return false;
        }

        String deviceClass = info.getDeviceClass();
        return "Desktop".equals(deviceClass);
    }

    /**
     * 获取平台类型（Mobile 或 Web）
     *
     * @param userAgentString User-Agent 字符串
     * @return Mobile 或 Web
     */
    public static String getPlatform(String userAgentString) {
        UserAgentInfo info = parse(userAgentString);
        if (info == null) {
            return "Web";
        }

        String deviceClass = info.getDeviceClass();
        if ("Phone".equals(deviceClass) || "Tablet".equals(deviceClass) || "Mobile".equals(deviceClass)) {
            return "Mobile";
        }
        return "Web";
    }

    /**
     * 获取设备/应用完整型号信息
     * 移动端返回：设备型号（如 iPhone 15 Pro、Samsung Galaxy S23）
     * PC端返回：浏览器+版本（如 Chrome 142、Edge 210）
     *
     * @param userAgentString User-Agent 字符串
     * @return 完整型号信息
     */
    public static String getModel(String userAgentString) {
        UserAgentInfo info = parse(userAgentString);
        if (info == null) {
            return "Unknown";
        }

        String deviceClass = info.getDeviceClass();

        // 移动设备：返回 设备名称
        if ("Phone".equals(deviceClass) || "Tablet".equals(deviceClass) || "Mobile".equals(deviceClass)) {
            String deviceName = info.getDeviceName();
            if (deviceName != null && !"Unknown".equals(deviceName)) {
                return deviceName;
            }
            String deviceBrand = info.getDeviceBrand();
            return deviceBrand != null ? deviceBrand : "Unknown";
        }

        // 桌面设备：返回 浏览器+版本
        String agentName = info.getAgentName();
        String agentVersion = info.getAgentVersion();
        if (agentName != null && !"Unknown".equals(agentName)) {
            if (agentVersion != null && !"Unknown".equals(agentVersion)) {
                // 获取主版本号（如 142.0.6099 -> 142）
                String majorVersion = agentVersion.split("\\.")[0];
                return agentName + " " + majorVersion;
            }
            return agentName;
        }

        return "Unknown";
    }

    /**
     * 获取浏览器名称和版本
     *
     * @param userAgentString User-Agent 字符串
     * @return 浏览器名称和版本（如：Chrome 120.0、Safari 17.4）
     */
    public static String getBrowserNameVersion(String userAgentString) {
        UserAgentInfo info = parse(userAgentString);
        if (info == null) {
            return "Unknown";
        }

        return info.getAgentNameVersion();
    }

    /**
     * 获取操作系统版本
     *
     * @param userAgentString User-Agent 字符串
     * @return 操作系统版本（如：Windows 10、iOS 17.4、Android 14）
     */
    public static String getOS(String userAgentString) {
        UserAgentInfo info = parse(userAgentString);
        if (info == null) {
            return "Unknown";
        }

        String osName = info.getOperatingSystemName();

        // Windows 特殊处理：直接从 UA 字符串提取 NT 版本号
        if ("Windows".equals(osName) || "Windows NT".equals(osName) || "WinNT".equals(osName)) {
            String ntVersion = extractWindowsNtVersion(userAgentString);
            if (ntVersion != null && !ntVersion.isEmpty()) {
                return mapWindowsVersion(ntVersion);
            }
            return "Windows";
        }

        // 其他系统直接返回名称+版本
        String nameVersion = info.getOperatingSystemNameVersion();
        return nameVersion != null && !nameVersion.isEmpty() ? nameVersion : osName;
    }

    /**
     * 从 UA 字符串中提取 Windows NT 版本号
     */
    private static String extractWindowsNtVersion(String userAgentString) {
        if (userAgentString == null) {
            return null;
        }
        // 匹配 Windows NT x.x 或 Windows NT x.x.x
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Windows\\s+NT\\s+(\\d+\\.\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(userAgentString);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 映射 Windows NT 版本号到具体版本名称
     */
    private static String mapWindowsVersion(String ntVersion) {
        if (ntVersion == null || ntVersion.isEmpty()) {
            return "Windows";
        }

        return switch (ntVersion) {
            case "10.0" -> "Windows 10/11";
            case "11.0" -> "Windows 11";
            case "6.3" -> "Windows 8.1";
            case "6.2" -> "Windows 8";
            case "6.1" -> "Windows 7";
            case "6.0" -> "Windows Vista";
            case "5.2" -> "Windows XP x64";
            case "5.1" -> "Windows XP";
            default -> "Windows " + ntVersion;
        };
    }

    /**
     * User-Agent 解析结果信息
     */
    @Data
    public static class UserAgentInfo {
        // ========== 设备信息 ==========
        /**
         * 设备类型（Phone、Tablet、Desktop、Mobile 等）
         */
        private String deviceClass;

        /**
         * 设备名称（如：iPhone、iPad、Samsung Galaxy S21）
         */
        private String deviceName;

        /**
         * 设备品牌（如：Apple、Samsung、Huawei）
         */
        private String deviceBrand;

        // ========== 操作系统信息 ==========
        /**
         * 操作系统类型（Mobile、Desktop、Cloud 等）
         */
        private String operatingSystemClass;

        /**
         * 操作系统名称（如：iOS、Android、Windows）
         */
        private String operatingSystemName;

        /**
         * 操作系统版本（如：17.4.1、14.0）
         */
        private String operatingSystemVersion;

        /**
         * 操作系统主版本号（如：17、14）
         */
        private String operatingSystemVersionMajor;

        /**
         * 操作系统名称和版本（如：iOS 17.4、Android 14）
         */
        private String operatingSystemNameVersion;

        // ========== 浏览器/应用信息 ==========
        /**
         * 应用类型（Browser、Mobile App、Robot 等）
         */
        private String agentClass;

        /**
         * 应用名称（如：Chrome、Safari、Firefox）
         */
        private String agentName;

        /**
         * 应用版本（如：120.0.6099.199）
         */
        private String agentVersion;

        /**
         * 应用主版本号（如：120）
         */
        private String agentVersionMajor;

        /**
         * 应用名称和版本（如：Chrome 120.0）
         */
        private String agentNameVersion;

        // ========== 布局引擎信息 ==========
        /**
         * 布局引擎类型
         */
        private String layoutEngineClass;

        /**
         * 布局引擎名称（如：Blink、WebKit、Gecko）
         */
        private String layoutEngineName;

        /**
         * 布局引擎版本
         */
        private String layoutEngineVersion;
    }
}
