package com.xigua.client.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xigua.client.dto.GroupFileSaveDTO;
import com.xigua.client.entity.FileAudit;
import com.xigua.client.entity.GroupFile;
import com.xigua.client.mapper.FileAuditMapper;
import com.xigua.client.mapper.GroupFileMapper;
import com.xigua.client.mapper.GroupMemberMapper;
import com.xigua.client.util.FileSecurityPolicy;
import com.xigua.common.core.base.R;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.entity.GroupMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 群文件中心：上传归档、列表、下载元数据、删除
 */
@Slf4j
@Tag(name = "群文件接口")
@RequestMapping("/group/file")
@RestController
public class GroupFileController {

    private static final int MAX_FILE_NAME = 200;

    private static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg");
    private static final List<String> DOC_EXTENSIONS = List.of("doc", "docx", "txt", "md", "rtf");
    private static final List<String> SHEET_EXTENSIONS = List.of("xls", "xlsx", "csv");
    private static final List<String> ARCHIVE_EXTENSIONS = List.of("zip", "rar", "7z", "tar", "gz");
    private static final List<String> VIDEO_EXTENSIONS = List.of("mp4", "mov", "avi", "mkv", "webm");
    private static final List<String> AUDIO_EXTENSIONS = List.of("mp3", "wav", "m4a", "aac", "flac");
    private static final List<String> ALL_FILE_EXTENSIONS = List.of(
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg",
            "pdf",
            "doc", "docx", "txt", "md", "rtf",
            "xls", "xlsx", "csv",
            "zip", "rar", "7z", "tar", "gz",
            "mp4", "mov", "avi", "mkv", "webm",
            "mp3", "wav", "m4a", "aac", "flac");

    @Autowired
    private GroupFileMapper groupFileMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Autowired
    private Sequence sequence;

    @Autowired
    private FileAuditMapper fileAuditMapper;

    @Operation(summary = "登记群文件")
    @PostMapping("/save")
    public R<GroupFile> save(@RequestBody GroupFileSaveDTO dto) {
        if (dto == null || dto.getGroupId() == null) {
            return R.fail("群id不能为空");
        }
        String fileName = StringUtils.trimToEmpty(dto.getFileName());
        if (StringUtils.isBlank(fileName)) {
            return R.fail("文件名不能为空");
        }
        if (fileName.length() > MAX_FILE_NAME) {
            return R.fail("文件名过长");
        }
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("\u0000")
                || fileName.contains("..")) {
            auditReject(dto.getGroupId(), currentUserId(), fileName, dto.getFileSize(), dto.getMimeType(),
                    "文件名包含非法字符");
            return R.fail("文件名包含非法字符");
        }
        String fileUrl = StringUtils.trimToEmpty(dto.getFileUrl());
        if (StringUtils.isBlank(fileUrl) || !(fileUrl.startsWith("http://") || fileUrl.startsWith("https://"))) {
            return R.fail("文件地址无效");
        }
        if (fileUrl.length() > 2048) {
            return R.fail("文件地址过长");
        }
        if (FileSecurityPolicy.exceedsSingleFileLimit(dto.getFileSize())) {
            return R.fail("单个文件不能超过 200MB");
        }
        Long userId = currentUserId();
        GroupMember member = findMember(dto.getGroupId(), userId);
        if (member == null) {
            return R.fail("你不在该群聊中");
        }

        String ext = FileSecurityPolicy.extensionOf(fileName);
        if (FileSecurityPolicy.isBlockedExtension(ext)) {
            auditReject(dto.getGroupId(), userId, fileName, dto.getFileSize(), dto.getMimeType(),
                    StringUtils.isBlank(ext) ? "缺少可信扩展名" : "可执行/脚本类文件不允许上传");
            return R.fail(StringUtils.isBlank(ext) ? "文件必须包含扩展名" : "出于安全考虑，不允许上传可执行或脚本类文件");
        }
        String mimeType = StringUtils.lowerCase(StringUtils.trimToEmpty(dto.getMimeType()), Locale.ROOT);
        if (FileSecurityPolicy.isBlockedMime(mimeType)) {
            auditReject(dto.getGroupId(), userId, fileName, dto.getFileSize(), mimeType, "危险 MIME 类型");
            return R.fail("文件内容类型不被允许");
        }
        if (dto.getFileSize() != null && dto.getFileSize() > 0) {
            Long used = groupFileMapper.sumSizeByUploader(String.valueOf(userId));
            if (FileSecurityPolicy.exceedsUserQuota(used == null ? 0L : used, dto.getFileSize())) {
                auditReject(dto.getGroupId(), userId, fileName, dto.getFileSize(), mimeType, "超出个人 2GB 配额");
                return R.fail("个人群文件空间已满（上限 2GB），请先删除历史文件");
            }
        }

        // 同一会话重复登记同一个地址时直接复用，避免刷新/重传产生重复条目
        GroupFile existing = groupFileMapper.selectOne(new LambdaQueryWrapper<GroupFile>()
                .eq(GroupFile::getGroupId, dto.getGroupId())
                .eq(GroupFile::getFileUrl, fileUrl)
                .last("limit 1"));
        if (existing != null) {
            return R.ok(existing, "文件已存在");
        }

        GroupFile file = new GroupFile();
        file.setId(sequence.nextNo());
        file.setGroupId(dto.getGroupId());
        file.setUploaderId(userId);
        file.setUploaderName(StringUtils.defaultIfBlank(
                UserContext.get() == null ? null : UserContext.get().getUserName(), "群成员"));
        file.setFileName(fileName);
        file.setFileUrl(fileUrl);
        file.setFileSize(dto.getFileSize());
        file.setMimeType(StringUtils.defaultIfBlank(dto.getMimeType(), "application/octet-stream"));
        file.setFileExt(ext);
        groupFileMapper.insert(file);
        audit(file, userId, "upload", "allow", null);
        return R.ok(file, "已保存到群文件");
    }

    @Operation(summary = "群文件列表")
    @GetMapping("/list")
    public R<Map<String, Object>> list(@RequestParam Long groupId,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String fileType,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = currentUserId();
        if (findMember(groupId, userId) == null) {
            return R.fail("你不在该群聊中");
        }
        backfillMissingFileExtensions(groupId);
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);
        LambdaQueryWrapper<GroupFile> wrapper = new LambdaQueryWrapper<GroupFile>()
                .eq(GroupFile::getGroupId, groupId)
                .orderByDesc(GroupFile::getCreateTime);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(GroupFile::getFileName, keyword.trim());
        }
        applyFileTypeFilter(wrapper, fileType);
        Page<GroupFile> page = groupFileMapper.selectPage(new Page<>(safePageNum, safePageSize), wrapper);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", safePageNum);
        result.put("pageSize", safePageSize);
        result.put("totalPage", (page.getTotal() + safePageSize - 1) / safePageSize);
        return R.ok(result, "查询成功");
    }

    @Operation(summary = "删除群文件")
    @PostMapping("/delete")
    public R<Boolean> delete(@RequestBody Map<String, Object> body) {
        Long fileId = body == null || body.get("fileId") == null ? null : Long.valueOf(String.valueOf(body.get("fileId")));
        if (fileId == null) {
            return R.fail("文件id不能为空");
        }
        GroupFile file = groupFileMapper.selectById(fileId);
        if (file == null) {
            return R.fail("文件不存在或已删除");
        }
        Long userId = currentUserId();
        GroupMember member = findMember(file.getGroupId(), userId);
        if (member == null) {
            return R.fail("你不在该群聊中");
        }
        boolean canDelete = Objects.equals(file.getUploaderId(), userId)
                || (member.getRole() != null && (member.getRole() == 1 || member.getRole() == 2));
        if (!canDelete) {
            return R.fail("只有上传者、群主或管理员可以删除");
        }
        groupFileMapper.deleteById(fileId);
        audit(file, userId, "delete", "allow", null);
        return R.ok(true, "已删除");
    }

    private void audit(GroupFile file, Long userId, String action, String result, String reason) {
        try {
            FileAudit audit = new FileAudit();
            audit.setId(sequence.nextNo());
            audit.setFileId(file.getId() == null ? null : String.valueOf(file.getId()));
            audit.setGroupId(file.getGroupId());
            audit.setUserId(String.valueOf(userId));
            audit.setFileName(file.getFileName());
            audit.setFileSize(file.getFileSize());
            audit.setMimeType(file.getMimeType());
            audit.setAction(action);
            audit.setResult(result);
            audit.setReason(reason);
            fileAuditMapper.insert(audit);
        } catch (Exception e) {
            log.warn("文件审计写入失败: {}", e.getMessage());
        }
    }

    private void auditReject(Long groupId, Long userId, String fileName, Long size, String mime,
                             String reason) {
        GroupFile file = new GroupFile();
        file.setGroupId(groupId);
        file.setFileName(fileName);
        file.setFileSize(size);
        file.setMimeType(mime);
        audit(file, userId, "upload", "reject", reason);
    }

    /**
     * 历史数据可能没有落 file_ext，首次查询该群时按文件名补一次，
     * 否则“图片/文档”等筛选项会漏掉老文件。
     */
    private void backfillMissingFileExtensions(Long groupId) {
        List<GroupFile> legacyFiles = groupFileMapper.selectList(new LambdaQueryWrapper<GroupFile>()
                .eq(GroupFile::getGroupId, groupId)
                .and(w -> w.isNull(GroupFile::getFileExt).or().eq(GroupFile::getFileExt, "")));
        for (GroupFile file : legacyFiles) {
            String ext = FileSecurityPolicy.extensionOf(file.getFileName());
            if (StringUtils.isBlank(ext)) {
                continue;
            }
            GroupFile update = new GroupFile();
            update.setId(file.getId());
            update.setFileExt(ext);
            groupFileMapper.updateById(update);
        }
    }

    private void applyFileTypeFilter(LambdaQueryWrapper<GroupFile> wrapper, String fileType) {
        String type = StringUtils.trimToEmpty(fileType).toLowerCase(Locale.ROOT);
        if (StringUtils.isBlank(type) || "all".equals(type)) {
            return;
        }
        if ("other".equals(type)) {
            wrapper.and(w -> w.isNull(GroupFile::getFileExt)
                    .or()
                    .notIn(GroupFile::getFileExt, ALL_FILE_EXTENSIONS));
            return;
        }
        List<String> extensions = switch (type) {
            case "image" -> IMAGE_EXTENSIONS;
            case "pdf" -> List.of("pdf");
            case "doc" -> DOC_EXTENSIONS;
            case "sheet" -> SHEET_EXTENSIONS;
            case "zip" -> ARCHIVE_EXTENSIONS;
            case "video" -> VIDEO_EXTENSIONS;
            case "audio" -> AUDIO_EXTENSIONS;
            default -> null;
        };
        if (extensions != null) {
            wrapper.in(GroupFile::getFileExt, extensions);
        }
    }

    private GroupMember findMember(Long groupId, Long userId) {
        return groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, String.valueOf(groupId))
                .eq(GroupMember::getUserId, String.valueOf(userId))
                .eq(GroupMember::getStatus, 1)
                .eq(GroupMember::getDelFlag, 0)
                .last("limit 1"));
    }

    private Long currentUserId() {
        if (UserContext.get() == null || StringUtils.isEmpty(UserContext.get().getUserId())) {
            throw new BusinessException("请先登录");
        }
        try {
            return Long.valueOf(UserContext.get().getUserId());
        } catch (NumberFormatException e) {
            throw new BusinessException("登录用户信息异常");
        }
    }
}
