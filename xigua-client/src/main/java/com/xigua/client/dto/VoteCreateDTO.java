package com.xigua.client.dto;

import lombok.Data;

import java.util.List;

/**
 * 发起群投票
 */
@Data
public class VoteCreateDTO {

    private Long groupId;

    private String title;

    private List<String> options;

    private Boolean multi;
}
