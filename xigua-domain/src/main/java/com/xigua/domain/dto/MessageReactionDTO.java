package com.xigua.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessageReactionDTO implements Serializable {
    private String messageId;
    private String emoji;
}
