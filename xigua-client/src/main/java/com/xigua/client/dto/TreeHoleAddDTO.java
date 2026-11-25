package com.xigua.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName TreeHoleAddDTO
 * @Description 发布树洞请求
 * @Author xigua
 * @Date 2026/10/16 19:30
 */
@Data
@Schema(title = "发布树洞请求")
public class TreeHoleAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 树洞内容（1-500字）
     */
    @Schema(description = "树洞内容")
    private String content;
}
