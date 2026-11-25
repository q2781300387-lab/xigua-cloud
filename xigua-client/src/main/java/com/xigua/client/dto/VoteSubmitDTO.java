package com.xigua.client.dto;

import lombok.Data;

/**
 * 提交投票
 */
@Data
public class VoteSubmitDTO {

    private Long voteId;

    /**
     * 选项下标，从 0 开始
     */
    private Integer optionIndex;
}
