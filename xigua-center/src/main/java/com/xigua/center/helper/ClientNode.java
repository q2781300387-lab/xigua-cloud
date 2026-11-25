package com.xigua.center.helper;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName ClientNode
 * @Description TODO
 * @Author wangjinfei
 * @Date 2026/1/5 22:16
 */
@Data
@Builder
public class ClientNode implements Serializable {
    private static final long serialVersionUID = 1L;
    // 客户端id
    private String clientId;
    // 内网ip
    private String ip;
    // 端口
    private Integer port;
    // 连接服务端的内网ip
    private String centerIp;

    // 负载 todo 后续可根据权重等字段分配客户端ws节点
    // 权重
//    private Integer weight;
}
