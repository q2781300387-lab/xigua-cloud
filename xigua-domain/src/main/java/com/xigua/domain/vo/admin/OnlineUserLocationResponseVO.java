package com.xigua.domain.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName OnlineUserLocationResponseVO
 * @Description 在线用户地理位置响应VO
 * @Author wangjinfei
 * @Date 2026/3/29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "在线用户地理位置响应")
public class OnlineUserLocationResponseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "在线用户位置列表")
    private List<OnlineUserLocationVO> onlineUsers;
}
