package com.xigua.domain.bo;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName MessageReaderBO
 * @Description 群聊消息的已读人（历史回填用，带 id 便于前端去重）
 * @Author wangjinfei
 * @Date 2026/11/12
 */
@Data
public class MessageReaderBO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 读者id
     */
    private String id;

    /**
     * 读者昵称
     */
    private String name;
}
