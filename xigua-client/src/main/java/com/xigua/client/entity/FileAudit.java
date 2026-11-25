package com.xigua.client.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("xg_file_audit")
public class FileAudit implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private String id;
    private String fileId;
    private Long groupId;
    private String userId;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String action;
    private String result;
    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
