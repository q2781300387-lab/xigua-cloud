package com.xigua.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MessageReactionQueryDTO implements Serializable {
    private List<String> messageIds;
}
