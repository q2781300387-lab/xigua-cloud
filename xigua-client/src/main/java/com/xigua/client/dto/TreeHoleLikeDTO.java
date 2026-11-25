package com.xigua.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName TreeHoleLikeDTO
 * @Description 树洞点赞请求
 * @Author xigua
 * @Date 2026/10/16 19:30
 */
@Data
@Schema(title = "树洞点赞请求")
public class TreeHoleLikeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 树洞id
     */
    @Schema(description = "树洞id")
    private String id;
}
