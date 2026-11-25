package com.xigua.common.core.util;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * IP 地址归属地查询工具类
 * 基于 ip2region xdb 格式数据文件实现
 *
 * @author wangjinfei
 * @date 2026/3/9
 */
@Slf4j
public class Ip2RegionUtil {

    private static final String DEFAULT_XDB_PATH = "ip2region_v4.xdb";
    static final Version version = Version.IPv4;
    private static Searcher searcher;

    static {
        try {
            initSearcher();
        } catch (IOException e) {
            log.error("Ip2RegionUtil init failed", e);
        }
    }

    /**
     * 初始化搜索器，优先从 classpath 加载，失败则从文件路径加载
     */
    private static void initSearcher() throws IOException {
        // 优先尝试从 classpath 加载（JAR 包环境需提取到临时文件）
        try {
            ClassPathResource resource = new ClassPathResource(DEFAULT_XDB_PATH);
            if (resource.exists()) {
                Path tempFile = extractToTempFile(resource);
                searcher = Searcher.newWithFileOnly(version, tempFile.toAbsolutePath().toString());
                log.info("Ip2RegionUtil initialized from classpath (temp file): {}", DEFAULT_XDB_PATH);
                return;
            }
        } catch (IOException e) {
            log.warn("Failed to load ip2region_v4.xdb from classpath, trying file path...", e);
        }

        // 尝试从文件路径加载（兼容本地开发场景）
        try {
            String dbPath = System.getProperty("ip2region.db.path", DEFAULT_XDB_PATH);
            searcher = Searcher.newWithFileOnly(version, dbPath);
            log.info("Ip2RegionUtil initialized from file path: {}", dbPath);
        } catch (IOException e) {
            log.error("Failed to initialize ip2region searcher from both classpath and file path", e);
            throw e;
        }
    }

    /**
     * 将 classpath 资源提取到临时文件
     */
    private static Path extractToTempFile(ClassPathResource resource) throws IOException {
        String suffix = ".xdb";
        Path tempFile = Files.createTempFile("ip2region_", suffix);
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        // JVM 退出时删除临时文件
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }

    /**
     * 根据IP地址查询归属地信息
     *
     * @param ip IP地址
     * @return 归属地信息，格式：中国|上海|上海市|电信|ISP
     */
    public static String searchMetaInfo(String ip) {
        if (searcher == null) {
            log.warn("Ip2Region searcher not initialized");
            return "unknown";
        }

        if (ip == null || ip.isEmpty()) {
            return "unknown";
        }

        // 本地回环地址直接返回 unknown
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "unknown";
        }

        try {
            return searcher.search(ip);
        } catch (Exception e) {
            log.warn("Failed to search ip: {}", ip, e);
            return "unknown";
        }
    }

    /**
     * 根据IP地址查询归属地信息（简化格式，只返回省市区）
     *
     * @param ip IP地址
     * @return 简化的归属地信息
     */
    public static String search(String ip) {
        String region = searchMetaInfo(ip);
        if (region == null || region.isEmpty()) {
            return "";
        }

        // 解析格式：中国|上海|上海市|电信|ISP
        // 返回：上海|上海市
        String[] parts = region.split("\\|");
        if (parts.length >= 3) {
            String area = parts[1];
            String province = parts[2];
            if ("0".equals(area)) {
                return province;
            }
            return area + "|" + province;
        }
        return region;
    }

    /**
     * 使用自定义路径重新初始化搜索器
     *
     * @param dbPath xdb 数据库文件路径
     */
    public static synchronized void reload(String dbPath) throws IOException {
        close();
        searcher = Searcher.newWithFileOnly(version, dbPath);
        log.info("Ip2RegionUtil reloaded from file: {}", dbPath);
    }

    /**
     * 关闭搜索器，释放资源
     */
    public static synchronized void close() {
        if (searcher != null) {
            try {
                searcher.close();
                log.info("Ip2RegionUtil searcher closed");
            } catch (IOException e) {
                log.warn("Failed to close ip2region searcher", e);
            } finally {
                searcher = null;
            }
        }
    }

    /**
     * 检查搜索器是否已初始化
     */
    public static boolean isReady() {
        return searcher != null;
    }

    public static void main(String[] args) {
        String search = search("127.0.0.1");
        String s = searchMetaInfo("127.0.0.1");
        System.out.println("s = " + s);
    }
}
