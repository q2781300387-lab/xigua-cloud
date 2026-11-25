package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName OnlineUserLocationVO
 * @Description 在线用户位置聚合VO
 * @Author wangjinfei
 * @Date 2026/3/29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "在线用户位置聚合信息")
public class OnlineUserLocationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "纬度")
    private Double lat;

    @Schema(description = "经度")
    private Double lng;

    @Schema(description = "国家")
    private String country;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "该位置在线用户数")
    private Integer userCount;

    @Schema(description = "用户详情列表")
    private List<OnlineUserInfoVO> users;
}
