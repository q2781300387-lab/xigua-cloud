package com.xigua.domain.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessageReactionItemVO implements Serializable {
    private String emoji;
    private Integer count;
    private Boolean mine;
}
