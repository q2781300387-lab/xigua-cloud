package com.xigua.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局搜索聚合结果：联系人、群、群文件。
 * 消息仍由 search 服务的 /chat/message/global 返回，前端按分区合并展示。
 */
@Data
@Schema(description = "全局搜索聚合结果")
public class GlobalSearchResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ContactItem> contacts = new ArrayList<>();
    private List<GroupItem> groups = new ArrayList<>();
    private List<FileItem> files = new ArrayList<>();

    @Data
    public static class ContactItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String name;
        private String avatar;
        private String signature;
        private Boolean online;
    }

    @Data
    public static class GroupItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String name;
        private String avatar;
        private Integer memberCount;
    }

    @Data
    public static class FileItem implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonSerialize(using = ToStringSerializer.class)
        private String id;
        @JsonSerialize(using = ToStringSerializer.class)
        private String groupId;
        private String groupName;
        private String name;
        private String url;
        private Long size;
        private String ext;
        private String uploaderName;
        private LocalDateTime createTime;
    }
}
