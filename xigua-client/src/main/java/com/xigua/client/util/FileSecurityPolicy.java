package com.xigua.client.util;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 群文件上传安全策略，抽出为纯函数便于单元测试。
 */
public final class FileSecurityPolicy {

    public static final long MAX_FILE_SIZE = 200L * 1024 * 1024;
    public static final long USER_TOTAL_QUOTA = 2L * 1024 * 1024 * 1024;

    public static final List<String> BLOCKED_EXTENSIONS = List.of(
            "exe", "bat", "cmd", "com", "sh", "js", "mjs", "jar", "apk", "ipa", "dll", "msi",
            "scr", "ps1", "vbs", "vb", "wsf", "wsh", "hta", "reg", "pif", "cpl", "lnk", "elf", "bin");

    public static final List<String> BLOCKED_MIME_TYPES = List.of(
            "application/x-msdownload", "application/x-msdos-program", "application/x-executable",
            "application/x-sh", "application/x-shellscript", "application/java-archive",
            "application/vnd.android.package-archive", "application/x-msi", "application/hta");

    private FileSecurityPolicy() {
    }

    public static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
        return ext.length() > 12 ? "" : ext;
    }

    public static boolean hasIllegalName(String fileName) {
        return fileName == null
                || fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains("\u0000")
                || fileName.contains("..");
    }

    public static boolean isBlockedExtension(String ext) {
        return StringUtils.isBlank(ext) || BLOCKED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }

    public static boolean isBlockedMime(String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            return false;
        }
        return BLOCKED_MIME_TYPES.contains(mimeType.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean exceedsUserQuota(long usedBytes, Long incomingBytes) {
        if (incomingBytes == null || incomingBytes <= 0) {
            return false;
        }
        return usedBytes + incomingBytes > USER_TOTAL_QUOTA;
    }

    public static boolean exceedsSingleFileLimit(Long fileSize) {
        return fileSize != null && fileSize > MAX_FILE_SIZE;
    }
}
