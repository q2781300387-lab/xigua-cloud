package com.xigua.api.service;

/**
 * @ClassName GroupAIService
 * @Description 群聊 AI 小助手 Dubbo 接口：群聊中 @ 机器人提问，返回 AI 回复文本
 * @Author xigua
 * @Date 2026/11/3 15:30
 */
public interface GroupAIService {

    /**
     * 让指定机器人回答群聊问题
     * @param botId    机器人id（xigua_bot 表）
     * @param question 群成员提出的问题
     * @return AI 回复文本（失败时返回兜底文案，不抛异常）
     */
    String chat(String botId, String question);

    /**
     * 根据群聊记录生成结构化摘要与待办
     * @param groupName 群名称
     * @param transcript 已拼接好的群聊记录（含发送人和时间）
     * @return 严格 JSON 文本：{"summary":"...","todos":[{"content":"...","owner":"...","due":"..."}]}
     */
    String summarize(String groupName, String transcript);
}
