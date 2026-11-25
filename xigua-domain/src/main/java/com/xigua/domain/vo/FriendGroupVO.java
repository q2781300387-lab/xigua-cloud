package com.xigua.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName FriendGroupVO
 * @Description 按字母分组的好友列表VO
 * @Author wangjinfei
 * @Date 2026/4/15
 */
@Data
@Schema(description = "按字母分组的好友列表")
public class FriendGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "首字母(A-Z或#)")
    private String letter;

    @Schema(description = "该字母下的好友列表")
    private List<FriendVO> list;
}
