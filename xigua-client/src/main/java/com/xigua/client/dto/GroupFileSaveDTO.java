package com.xigua.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(title = "保存群文件请求参数")
public class GroupFileSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long groupId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
}
