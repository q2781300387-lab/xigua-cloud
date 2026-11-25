package com.xigua.common.core.util;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.text.DecimalFormat;

/**
 * @ClassName SystemInfoUtil
 * @Description 系统信息工具类 - 基于OSHI获取系统硬件和OS信息
 * @Author wangjinfei
 * @Date 2026/3/10
 */
public class SystemInfoUtil {

    private static final SystemInfo SYSTEM_INFO = new SystemInfo();
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    /**
     * 获取CPU使用率
     * @author wangjinfei
     * @date 2026/3/10
     * @return double CPU使用率百分比
     */
    public static double getCpuUsage() {
        HardwareAbstractionLayer hardware = SYSTEM_INFO.getHardware();
        CentralProcessor processor = hardware.getProcessor();

        // 采集初始 ticks
        long[] prevTicks = processor.getSystemCpuLoadTicks();

        // 等待1秒
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }

        // 采集当前 ticks
        long[] ticks = processor.getSystemCpuLoadTicks();

        // 计算CPU使用率
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long total = user + nice + cSys + idle + iowait + irq + softirq + steal;

        return total == 0 ? 0 : (100d * (total - idle) / total);
    }

    /**
     * 获取内存使用率
     * @author wangjinfei
     * @date 2026/3/10
     * @return double 内存使用率百分比
     */
    public static double getMemoryUsage() {
        HardwareAbstractionLayer hardware = SYSTEM_INFO.getHardware();
        GlobalMemory memory = hardware.getMemory();

        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;

        return total == 0 ? 0 : (100d * used / total);
    }

    /**
     * 获取内存信息（格式化字符串）
     * @author wangjinfei
     * @date 2026/3/10
     * @return String 内存信息
     */
    public static String getMemoryInfo() {
        HardwareAbstractionLayer hardware = SYSTEM_INFO.getHardware();
        GlobalMemory memory = hardware.getMemory();

        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;

        return "总内存: " + formatBytes(total) + ", 已用: " + formatBytes(used) + ", 可用: " + formatBytes(available);
    }

    /**
     * 获取JVM内存使用率
     * @author wangjinfei
     * @date 2026/3/10
     * @return double JVM内存使用率百分比
     */
    public static double getJvmMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return maxMemory == 0 ? 0 : (100d * usedMemory / maxMemory);
    }

    /**
     * 获取JVM内存信息（格式化字符串）
     * @author wangjinfei
     * @date 2026/3/10
     * @return String JVM内存信息
     */
    public static String getJvmMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return "最大内存: " + formatBytes(maxMemory) + ", 已分配: " + formatBytes(totalMemory) +
               ", 已用: " + formatBytes(usedMemory) + ", 空闲: " + formatBytes(freeMemory);
    }

    /**
     * 获取操作系统信息
     * @author wangjinfei
     * @date 2026/3/10
     * @return String 操作系统信息
     */
    public static String getOsInfo() {
        OperatingSystem os = SYSTEM_INFO.getOperatingSystem();
        return "操作系统: " + os.getFamily() + " " + os.getVersionInfo().getVersion() +
               ", 架构: " + os.getBitness() + "位";
    }

    /**
     * 获取CPU信息
     * @author wangjinfei
     * @date 2026/3/10
     * @return String CPU信息
     */
    public static String getCpuInfo() {
        HardwareAbstractionLayer hardware = SYSTEM_INFO.getHardware();
        CentralProcessor processor = hardware.getProcessor();

        return "CPU: " + processor.getProcessorIdentifier().getName() +
               ", 核心数: " + processor.getPhysicalProcessorCount() +
               ", 逻辑处理器: " + processor.getLogicalProcessorCount();
    }

    /**
     * 获取CPU核心数（逻辑处理器数）
     * @author wangjinfei
     * @date 2026/3/18
     * @return int CPU核心数
     */
    public static int getCpuCores() {
        HardwareAbstractionLayer hardware = SYSTEM_INFO.getHardware();
        CentralProcessor processor = hardware.getProcessor();
        return processor.getLogicalProcessorCount();
    }

    /**
     * 获取总内存大小（字节）
     * @author wangjinfei
     * @date 2026/3/18
     * @return long 总内存大小（字节）
     */
    public static long getTotalMemory() {
        HardwareAbstractionLayer hardware = SYSTEM_INFO.getHardware();
        GlobalMemory memory = hardware.getMemory();
        return memory.getTotal();
    }

    /**
     * 格式化字节数
     * @author wangjinfei
     * @date 2026/3/10
     * @param bytes 字节数
     * @return String 格式化后的字符串
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return DF.format(bytes / 1024.0) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return DF.format(bytes / (1024.0 * 1024)) + " MB";
        } else if (bytes < 1024L * 1024 * 1024 * 1024) {
            return DF.format(bytes / (1024.0 * 1024 * 1024)) + " GB";
        } else {
            return DF.format(bytes / (1024.0 * 1024 * 1024 * 1024)) + " TB";
        }
    }

    /**
     * 获取系统信息摘要
     * @author wangjinfei
     * @date 2026/3/10
     * @return java.util.Map<String, Object>
     */
    public static java.util.Map<String, Object> getSystemSummary() {
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("cpuUsage", String.format("%.2f%%", getCpuUsage()));
        summary.put("memoryUsage", String.format("%.2f%%", getMemoryUsage()));
        summary.put("jvmMemoryUsage", String.format("%.2f%%", getJvmMemoryUsage()));
        summary.put("memoryInfo", getMemoryInfo());
        summary.put("jvmMemoryInfo", getJvmMemoryInfo());
        summary.put("osInfo", getOsInfo());
        summary.put("cpuInfo", getCpuInfo());
        return summary;
    }
}
