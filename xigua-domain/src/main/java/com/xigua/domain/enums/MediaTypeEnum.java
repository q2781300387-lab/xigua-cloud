package com.xigua.domain.enums;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public enum MediaTypeEnum {
    // text/* 类型
    TEXT_HTML("text/html", "html"),
    TEXT_PLAIN("text/plain", "txt"),
    TEXT_XML("text/xml", "xml"),
    TEXT_CSV("text/csv", "csv"),
    TEXT_MARKDOWN("text/markdown", "md"),

    // image/* 类型
    IMAGE_GIF("image/gif", "gif"),
    IMAGE_JPEG("image/jpeg", "jpg"),
    IMAGE_PNG("image/png", "png"),

    // 音频
    AUDIO_MP3("audio/mpeg", "mp3"),
    AUDIO_WAV("audio/wav", "wav"),
    AUDIO_OGG("audio/ogg", "ogg"),
    AUDIO_M4A("audio/mp4", "m4a"),

    // 视频
    VIDEO_MP4("video/mp4", "mp4"),
    VIDEO_WEBM("video/webm", "webm"),
    VIDEO_MOV("video/quicktime", "mov"),

    // application/* 类型
    APPLICATION_XHTML_XML("application/xhtml+xml", "xhtml"),
    APPLICATION_XML("application/xml", "xml"),
    APPLICATION_ATOM_XML("application/atom+xml", "atom"),
    APPLICATION_JSON("application/json", "json"),
    APPLICATION_PDF("application/pdf", "pdf"),
    APPLICATION_MSWORD("application/msword", "doc"),
    APPLICATION_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    APPLICATION_XLS("application/vnd.ms-excel", "xls"),
    APPLICATION_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    APPLICATION_PPT("application/vnd.ms-powerpoint", "ppt"),
    APPLICATION_PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
    APPLICATION_ZIP("application/zip", "zip"),
    APPLICATION_X_ZIP_COMPRESSED("application/x-zip-compressed", "zip"),
    APPLICATION_OCTET_STREAM("application/octet-stream", ""),
    APPLICATION_X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded", "");

    private final String contentType;
    private final String fileSuffix;

    public String getContentType() {
        return contentType;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public static MediaTypeEnum getBySuffix(String suffix) {
        for (MediaTypeEnum mediaType : MediaTypeEnum.values()) {
            if (mediaType.getFileSuffix().equals(suffix)) {
                return mediaType;
            }
        }
        return null;
    }
}
