package com.xigua.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class MessageInteractionVO implements Serializable {
    private List<MessageReactionSummaryVO> reactions = new ArrayList<>();
    private List<String> favoriteIds = new ArrayList<>();
}
