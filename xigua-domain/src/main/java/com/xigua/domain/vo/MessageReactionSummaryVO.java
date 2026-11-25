package com.xigua.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class MessageReactionSummaryVO implements Serializable {
    private String messageId;
    private List<MessageReactionItemVO> items = new ArrayList<>();
}
