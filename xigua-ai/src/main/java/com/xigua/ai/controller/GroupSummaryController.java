package com.xigua.ai.controller;

import com.xigua.ai.domain.dto.GroupSummaryRequest;
import com.xigua.ai.llm.LLMService;
import com.xigua.ai.llm.model.ChatContext;
import com.xigua.common.core.base.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName GroupSummaryController
 * @Description 群聊智能摘要：对最近消息生成要点摘要
 * @Author xigua
 * @Date 2026/10/16 19:30
 */
@Slf4j
@Tag(name = "群聊智能摘要接口")
@RequestMapping("/chat/summary")
@RestController
public class GroupSummaryController {

    @Autowired
    private LLMService llmService;

    private static final String SUMMARY_PROMPT =
            "你是一名校园社交平台的会话摘要助手。请根据下面给出的聊天消息列表，"
                    + "提炼出3-5句精炼的中文摘要，覆盖：聊了什么话题、讨论出的结论或共识、"
                    + "提到的待办或约定、整体氛围。用简洁的口语化表达，不要逐条复述消息，"
                    + "不要出现编号，直接输出摘要正文。";

    @Operation(summary = "生成会话智能摘要")
    @PostMapping
    public R<String> summary(@RequestBody GroupSummaryRequest request) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            return R.fail("消息列表不能为空");
        }
        // 截取最近 50 条，拼成消息文本
        List<GroupSummaryRequest.SummaryMessage> msgs = request.getMessages();
        if (msgs.size() > 50) {
            msgs = msgs.subList(msgs.size() - 50, msgs.size());
        }
        String scene = request.getChatType() != null && request.getChatType() == 2 ? "（群聊场景）" : "（私聊场景）";
        String input = scene + "消息列表如下：\n" + msgs.stream()
                .map(m -> (StringUtils.isBlank(m.getName()) ? "匿名" : m.getName()) + "：" + (m.getContent() == null ? "" : m.getContent()))
                .collect(Collectors.joining("\n"));
        try {
            ChatContext chatContext = ChatContext.builder()
                    .input(input)
                    .stream(false)
                    .prompt(SUMMARY_PROMPT)
                    .build();
            Flux<String> flux = llmService.chat(chatContext);
            List<String> chunks = flux.collectList().block(Duration.ofSeconds(30));
            String text = chunks == null ? "" : String.join("", chunks).trim();
            if (StringUtils.isBlank(text)) {
                return R.ok(fallbackSummary(msgs.size()), "生成失败，已返回统计摘要");
            }
            return R.ok(text, "摘要生成成功");
        } catch (Exception e) {
            log.error("生成会话摘要失败", e);
            return R.ok(fallbackSummary(msgs.size()), "生成失败，已返回统计摘要");
        }
    }

    /**
     * 兜底：模型不可用时返回统计式摘要（不编造内容）
     */
    private String fallbackSummary(int count) {
        return "本次共浏览 " + count + " 条消息，建议重新生成以获取 AI 智能摘要。";
    }
}
