package com.xigua.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileSecurityPolicyTest {

    @Test
    void extractsLowercaseExtensionAndRejectsTrailingDot() {
        assertEquals("txt", FileSecurityPolicy.extensionOf("report.TXT"));
        assertEquals("gz", FileSecurityPolicy.extensionOf("archive.tar.gz"));
        assertEquals("", FileSecurityPolicy.extensionOf("noext"));
        assertEquals("", FileSecurityPolicy.extensionOf("trailing."));
        assertEquals("", FileSecurityPolicy.extensionOf(null));
    }

    @Test
    void blocksExecutableAndScriptExtensions() {
        for (String ext : new String[]{"exe", "bat", "cmd", "sh", "js", "jar", "apk", "dll", "msi", "ps1", "vbs"}) {
            assertTrue(FileSecurityPolicy.isBlockedExtension(ext), ext + " should be blocked");
        }
        assertTrue(FileSecurityPolicy.isBlockedExtension(""));
        assertFalse(FileSecurityPolicy.isBlockedExtension("txt"));
        assertFalse(FileSecurityPolicy.isBlockedExtension("pdf"));
    }

    @Test
    void detectsDangerousMimeTypesCaseInsensitively() {
        assertTrue(FileSecurityPolicy.isBlockedMime("application/x-msdownload"));
        assertTrue(FileSecurityPolicy.isBlockedMime(" APPLICATION/X-EXECUTABLE "));
        assertFalse(FileSecurityPolicy.isBlockedMime("text/plain"));
        assertFalse(FileSecurityPolicy.isBlockedMime(null));
    }

    @Test
    void rejectsPathTraversalNames() {
        assertTrue(FileSecurityPolicy.hasIllegalName("../etc/passwd"));
        assertTrue(FileSecurityPolicy.hasIllegalName("a\\b.txt"));
        assertTrue(FileSecurityPolicy.hasIllegalName("a\u0000b.txt"));
        assertFalse(FileSecurityPolicy.hasIllegalName("normal file.txt"));
    }

    @Test
    void enforcesSingleFileAndUserQuota() {
        assertTrue(FileSecurityPolicy.exceedsSingleFileLimit(FileSecurityPolicy.MAX_FILE_SIZE + 1));
        assertFalse(FileSecurityPolicy.exceedsSingleFileLimit(FileSecurityPolicy.MAX_FILE_SIZE));
        assertTrue(FileSecurityPolicy.exceedsUserQuota(
                FileSecurityPolicy.USER_TOTAL_QUOTA - 1, 2L));
        assertFalse(FileSecurityPolicy.exceedsUserQuota(0, 1024L));
        assertFalse(FileSecurityPolicy.exceedsUserQuota(FileSecurityPolicy.USER_TOTAL_QUOTA, 0L));
    }
}
