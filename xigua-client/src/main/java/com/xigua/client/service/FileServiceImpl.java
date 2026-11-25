package com.xigua.client.service;

import com.xigua.api.service.UserResourceQuotaService;
import com.xigua.api.service.UserResourceService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.thread.ThreadMgr;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.dto.MultipartFileDTO;
import com.xigua.domain.entity.UserResource;
import com.xigua.domain.entity.UserResourceQuota;
import com.xigua.domain.enums.MediaTypeEnum;
import com.xigua.api.service.FileService;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @ClassName FileServiceImpl
 * @Description
 * @Author wangjinfei
 * @Date 2025/5/8 13:54
 */
@Slf4j
@Service
@DubboService
public class FileServiceImpl implements FileService {
    // 允许上传的文件后缀名
    private final static Set<String> ALLOWED_EXTENSIONS = Set.of(
            // images
            "jpg", "jpeg", "png", "gif", "webp",

            // audio
            "mp3", "wav", "aac", "m4a", "ogg",

            // video
            "mp4", "mov", "avi", "webm",

            // document
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "json", "xml", "md",

            // archive
            "zip"
    );

    @Value("${minio.bucket-name}")
    private String bucketName;
    @Value("${minio.proxy}")
    private String proxy;

    @Autowired
    private Sequence sequence;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private UserResourceService userResourceService;
    @Autowired
    private UserResourceQuotaService userResourceQuotaService;


    /**
     * 上传文件
     * @author wangjinfei
     * @date 2025/5/8 13:53
     * @param multipartFileDTO
     * @return String
     */
    @Override
    public String upload(MultipartFileDTO multipartFileDTO) {
        String originalFilename = multipartFileDTO.getOriginalFilename();
        String contentType = multipartFileDTO.getContentType();
        byte[] bytes = multipartFileDTO.getBytes();
        InputStream inputStream = new ByteArrayInputStream(bytes);

        // 校验文件
        checkFile(multipartFileDTO);

        // 开关控制
        // 校验用户容量
//        checkQuota(multipartFileDTO);

        String url;
        try {
            // 当前日期作为目录
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            String dateStr = currentDate.format(formatter);

            // 文件key = 日期目录 + 序号_文件名 （序号防止目录下有重复文件名）
            // 只取文件名本身，剥掉任何客户端带过来的路径片段，避免对象存储里出现 ../ 之类的键
            String safeFilename = FilenameUtils.getName(originalFilename);
            String fileKey = dateStr + "/" + sequence.nextNo() + "_" + safeFilename ;

            // minio上传
            ObjectWriteResponse response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileKey)
                            .contentType(contentType)
                            .stream(inputStream, multipartFileDTO.getSize(), -1)
                            .build());

            // 用代理前缀拼接访问地址  nginx做了代理会直接访问minio服务器
            url = proxy + "/" + bucketName + "/" + fileKey;
        } catch (Exception e) {
            log.error("上传失败", e);
            return "上传失败";
        }

        Executor executor = ThreadMgr.getExecutor();
        CompletableFuture.runAsync(() -> insertResource(multipartFileDTO, url), executor);
        return url;
    }

    /**
     * 校验文件
     * @author wangjinfei
     * @date 2025/6/1 3:16
     * @param multipartFileDTO
    */
    private void checkFile(MultipartFileDTO multipartFileDTO){
        String originalFilename = FilenameUtils.getName(multipartFileDTO.getOriginalFilename());
        String contentType = multipartFileDTO.getContentType();
        String suffix = FilenameUtils.getExtension(originalFilename);

        if (StringUtils.isEmpty(originalFilename) || originalFilename.length() > 200) {
            throw new BusinessException("文件名不合法");
        }

        // 校验文件大小
        if(multipartFileDTO.getSize() == 0){
            throw new BusinessException("文件为空");
        }
        // 聊天文件上传上限 100MB，避免超大文件打满磁盘与带宽
        if(multipartFileDTO.getSize() > 100L * 1024 * 1024){
            throw new BusinessException("文件不能超过 100MB");
        }

        // 校验文件名称，防止伪装脚本(111.php.jpg)
        if (originalFilename.matches("(?i).+\\.(php|jsp|asp|aspx|exe|sh|html|js)(\\..*)?")) {
            throw new BusinessException("非法文件名");
        }

        // 检查文件后缀名
        if (!ALLOWED_EXTENSIONS.contains(suffix)) {
            throw new BusinessException("不允许的文件类型");
        }

        // 检测媒体类型
        MediaTypeEnum mediaTypeE = MediaTypeEnum.getBySuffix(suffix);
        if(mediaTypeE == null){
            throw new BusinessException("不允许的文件类型");
        }
        // 浏览器对同一种后缀可能给出不同的 Content-Type（如 zip、md），按别名放行
        if (!mediaTypeE.getContentType().equals(contentType) && !isAliasContentType(suffix, contentType)) {
            throw new BusinessException("不允许的文件类型");
        }

        // 魔数校验：防止把可执行内容伪装成图片/文档扩展名
        checkMagicBytes(multipartFileDTO.getBytes(), suffix);

        /**
         * todo 文件内容检测，暂时不做
         * 一般云厂商会提供文件内容检测服务，阿里云的oss、腾讯云cos、华为云obs、亚马逊云s3
        */
    }

    private void checkMagicBytes(byte[] bytes, String suffix) {
        if (bytes == null || bytes.length < 4) {
            throw new BusinessException("文件内容不合法");
        }
        String ext = suffix.toLowerCase();
        boolean ok;
        switch (ext) {
            case "jpg":
            case "jpeg":
                ok = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
                break;
            case "png":
                ok = (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;
                break;
            case "gif":
                ok = bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8';
                break;
            case "webp":
                ok = bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                        && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
                break;
            case "pdf":
                ok = bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
                break;
            case "zip":
            case "docx":
            case "xlsx":
            case "pptx":
                ok = bytes[0] == 'P' && bytes[1] == 'K' && (bytes[2] == 3 || bytes[2] == 5 || bytes[2] == 7);
                break;
            case "mp4":
            case "mov":
            case "m4a":
                ok = bytes.length >= 8 && bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p';
                break;
            case "avi":
                ok = bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                        && bytes[8] == 'A' && bytes[9] == 'V' && bytes[10] == 'I';
                break;
            case "webm":
                ok = bytes.length >= 4 && (bytes[0] & 0xFF) == 0x1A && (bytes[1] & 0xFF) == 0x45
                        && (bytes[2] & 0xFF) == 0xDF && (bytes[3] & 0xFF) == 0xA3;
                break;
            case "mp3":
                ok = (bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3')
                        || ((bytes[0] & 0xFF) == 0xFF && ((bytes[1] & 0xE0) == 0xE0));
                break;
            case "wav":
                ok = bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                        && bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E';
                break;
            case "ogg":
                ok = bytes[0] == 'O' && bytes[1] == 'g' && bytes[2] == 'g' && bytes[3] == 'S';
                break;
            case "aac":
                ok = (bytes[0] & 0xFF) == 0xFF && (((bytes[1] & 0xFF) == 0xF1) || ((bytes[1] & 0xFF) == 0xF9));
                break;
            default:
                // txt/csv/json/xml/md 无固定魔数，由扩展名 + Content-Type 控制
                ok = true;
        }
        if (!ok) {
            throw new BusinessException("文件内容与扩展名不匹配");
        }
    }

    // 浏览器同后缀可能给出多个 Content-Type，放行常见别名，文件名与后缀校验仍然生效
    private boolean isAliasContentType(String suffix, String contentType) {
        if (contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase();
        switch (suffix.toLowerCase()) {
            case "zip":
                return "application/x-zip-compressed".equals(lower) || "application/octet-stream".equals(lower);
            case "txt":
            case "csv":
            case "md":
                return "text/plain".equals(lower) || "application/octet-stream".equals(lower);
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
            case "doc":
            case "docx":
                return "application/octet-stream".equals(lower);
            case "webm":
                // 浏览器录音 MediaRecorder 产出 audio/webm;codecs=opus，同后缀也用于视频
                return lower.startsWith("audio/webm") || lower.startsWith("video/webm");
            default:
                return false;
        }
    }

    private void checkQuota(MultipartFileDTO multipartFileDTO){
        String userId = UserContext.get().getUserId();
        Long fileSize = multipartFileDTO.getSize();

        UserResourceQuota quota = userResourceQuotaService.getByUserId(userId);
        if (quota == null) {
            throw new BusinessException("用户资源配额未初始化");
        }

        // 单文件大小校验
        if (fileSize > quota.getMaxFileSize()) {
            throw new BusinessException("单文件大小超过限制");
        }

        // 文件本身超过总配额
        if (fileSize > quota.getMaxTotalSize()) {
            throw new BusinessException("文件大小已超过用户总存储配额");
        }

        // 总容量预校验
        if (quota.getUsedSize() + fileSize > quota.getMaxTotalSize()) {
            throw new BusinessException("用户存储空间不足");
        }
    }

    private void insertResource(MultipartFileDTO multipartFileDTO, String url){
        String originalFilename = multipartFileDTO.getOriginalFilename();
        Long fileSize = multipartFileDTO.getSize();
        String fileType = FilenameUtils.getExtension(originalFilename);
        String userId = UserContext.get().getUserId();

        // 插入用户资源
        UserResource resource = new UserResource();
        resource.setId(sequence.nextNo());
        resource.setUserId(userId);
        resource.setSize(fileSize);
        resource.setFileType(fileType);
        resource.setUrl(url);
        userResourceService.save(resource);


        // 更新用户资源统计
        UserResourceQuota quota = userResourceQuotaService.getByUserId(userId);
        if (quota == null) {
            throw new BusinessException("用户资源配额不存在");
        }

        quota.setUsedSize(quota.getUsedSize() + fileSize);
        quota.setUsedCount(quota.getUsedCount() + 1);
        userResourceQuotaService.updateById(quota);
    }
}
