package com.xigua.common.core.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.icmp4j.IcmpPingRequest;
import org.icmp4j.IcmpPingResponse;
import org.icmp4j.IcmpPingUtil;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * @ClassName NetUtil
 * @Description
 * @Author wangjinfei
 * @Date 2025/10/23 15:43
 */
@Slf4j
public class NetUtil {
    private static final String NIC_NAME = System.getProperty("network.nic-name");
    private static final String[] HEADERS_TO_CHECK = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
    };

    /**
     * 常用公共 DNS 服务器列表，用于网络延迟测试
     * 按优先级排序：优先使用国内 DNS
     */
    private static final String[] PUBLIC_DNS_SERVERS = {
            "114.114.114.114",  // 国内常用 DNS
            "223.5.5.5",        // 阿里 DNS
            "8.8.8.8",          // Google DNS
            "1.1.1.1"           // Cloudflare DNS
    };

    public static String getLocalAddress() {
        if(StringUtils.isNotEmpty(NIC_NAME)){
            return getIpByNic(NIC_NAME);
        }
        return getLocalIp();
    }

    public static String getIpByNic(String nicName) {
        try {
            NetworkInterface networkInterface = NetworkInterface.getByName(nicName);
            if (networkInterface == null) {
                return null;
            }
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                    return addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return null;
    }

    public static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static String getClientIp(){
        try {
            // 动态加载 Spring Web 的 RequestContextHolder
            Class<?> holderClass = Class.forName("org.springframework.web.context.request.RequestContextHolder");
            Class<?> attrClass = Class.forName("org.springframework.web.context.request.ServletRequestAttributes");
            Object attrs = holderClass.getMethod("getRequestAttributes").invoke(null);
            if (attrs == null) {
                return "unknown";
            }
            Object request = attrClass.getMethod("getRequest").invoke(attrs);
            return getClientIp((HttpServletRequest) request);
        } catch (Throwable e) {
            // 如果没有 Spring Web，或在非请求上下文环境中
            return "unknown";
        }
    }

    /**
     * 获取客户端真实IP（自动处理代理和多级转发）
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // 1. 按优先级检查各个代理头
        String ip = parseProxyHeaders(request);

        // 2. 如果代理头未找到有效IP，使用远程地址
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 3. 处理多级代理（取第一个非unknown的IP）
        ip = splitFirstIp(ip);

        // 4. 处理IPv6本地地址
        return normalizeLocalIp(ip);
    }

    private static String parseProxyHeaders(HttpServletRequest request) {
        for (String header : HEADERS_TO_CHECK) {
            String ip = request.getHeader(header);
            if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return null;
    }

    private static String splitFirstIp(String ip) {
        if (ip != null && ip.contains(",")) {
            return ip.split(",")[0].trim();
        }
        return ip;
    }

    private static String normalizeLocalIp(String ip) {
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    /**
     * 获取指定主机的网络延迟（ping），使用默认超时 5000ms
     *
     * @param host 主机地址（IP 或域名）
     * @return 延迟时间（毫秒），如果 ping 失败返回 -1
     */
    public static long getPingLatency(String host) {
        return getPingLatency(host, 5000);
    }

    /**
     * 获取指定主机的网络延迟（ping）
     *
     * @param host    主机地址（IP 或域名）
     * @param timeout 超时时间（毫秒）
     * @return 延迟时间（毫秒），如果 ping 失败返回 -1
     */
    public static long getPingLatency(String host, int timeout) {
        if (StringUtils.isBlank(host)) {
            log.warn("getPingLatency: host is blank");
            return -1;
        }

        try {
            IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest();
            request.setHost(host);
            request.setTimeout(timeout);

            IcmpPingResponse response = IcmpPingUtil.executePingRequest(request);

            if (response.getSuccessFlag()) {
                return response.getDuration();
            } else {
                log.warn("Ping failed for host: {}, error: {}", host, response.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Failed to ping host: {}", host, e);
        }

        return -1;
    }

    /**
     * 获取指定主机的完整 ping 响应信息
     *
     * @param host    主机地址（IP 或域名）
     * @param timeout 超时时间（毫秒）
     * @return IcmpPingResponse 对象，包含详细的 ping 信息
     */
    public static IcmpPingResponse getPingResponse(String host, int timeout) {
        if (StringUtils.isBlank(host)) {
            log.warn("getPingResponse: host is blank");
            return null;
        }

        try {
            IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest();
            request.setHost(host);
            request.setTimeout(timeout);

            return IcmpPingUtil.executePingRequest(request);
        } catch (Exception e) {
            log.error("Failed to get ping response for host: {}", host, e);
            return null;
        }
    }

    /**
     * 检查指定主机是否可达（ping 通）
     *
     * @param host    主机地址（IP 或域名）
     * @param timeout 超时时间（毫秒）
     * @return true 表示可达，false 表示不可达
     */
    public static boolean isHostReachable(String host, int timeout) {
        return getPingLatency(host, timeout) > 0;
    }

    /**
     * 检查指定主机是否可达（ping 通），使用默认超时 5000ms
     *
     * @param host 主机地址（IP 或域名）
     * @return true 表示可达，false 表示不可达
     */
    public static boolean isHostReachable(String host) {
        return isHostReachable(host, 5000);
    }

    /**
     * 获取当前网络延迟
     * 通过 ping 常用公共 DNS 服务器来测试网络延迟
     * 优先使用国内 DNS（114.114.114.114），失败时尝试 Google DNS（8.8.8.8）
     *
     * @return 网络延迟（毫秒），如果所有测试都失败返回 -1
     */
    public static long getCurrentNetworkLatency() {
        return getCurrentNetworkLatency(3000);
    }

    /**
     * 获取当前网络延迟
     * 通过 ping 常用公共 DNS 服务器来测试网络延迟
     * 优先使用国内 DNS（114.114.114.114），失败时尝试 Google DNS（8.8.8.8）
     *
     * @param timeout 超时时间（毫秒）
     * @return 网络延迟（毫秒），如果所有测试都失败返回 -1
     */
    public static long getCurrentNetworkLatency(int timeout) {
        // 尝试 ping 每个 DNS 服务器，返回第一个成功的结果
        for (String dns : PUBLIC_DNS_SERVERS) {
            long latency = getPingLatency(dns, timeout);
            if (latency > 0) {
                log.debug("Network latency test successful using DNS: {}, latency: {}ms", dns, latency);
                return latency;
            }
        }

        log.warn("Failed to get current network latency, all DNS servers unreachable");
        return -1;
    }

    /**
     * 获取当前网络延迟的平均值
     * 通过 ping 多个公共 DNS 服务器并计算平均延迟
     *
     * @param timeout 超时时间（毫秒）
     * @return 平均网络延迟（毫秒），如果所有测试都失败返回 -1
     */
    public static long getAverageNetworkLatency(int timeout) {
        long totalLatency = 0;
        int successCount = 0;

        for (String dns : PUBLIC_DNS_SERVERS) {
            long latency = getPingLatency(dns, timeout);
            if (latency > 0) {
                totalLatency += latency;
                successCount++;
            }
        }

        if (successCount > 0) {
            long avgLatency = totalLatency / successCount;
            log.debug("Average network latency: {}ms (tested {} servers)", avgLatency, successCount);
            return avgLatency;
        }

        log.warn("Failed to get average network latency, all DNS servers unreachable");
        return -1;
    }
}
