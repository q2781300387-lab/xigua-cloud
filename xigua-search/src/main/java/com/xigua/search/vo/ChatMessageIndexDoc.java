package com.xigua.search.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * @ClassName ChatMessageIndexDoc
 * @Description es 聊天消息文档的轻量映射，只用来读增量对账的水位线（createTime），
 *              其余字段忽略，避免反序列化时因为字段增减失败
 * @Author wangjinfei
 * @Date 2026/11/12
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessageIndexDoc {

    /**
     * 消息id
     */
    private String id;

    /**
     * 消息创建时间（yyyy-MM-dd HH:mm:ss）
     */
    private String createTime;
}
