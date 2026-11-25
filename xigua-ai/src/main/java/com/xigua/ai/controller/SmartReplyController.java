package com.xigua.ai.controller;

import com.xigua.ai.domain.dto.SmartReplyRequest;
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
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName SmartReplyController
 * @Description AI 智能回复：根据收到的消息生成多条回复建议
 * @Author xigua
 * @Date 2026/10/16 19:30
 */
@Slf4j
@Tag(name = "AI智能回复接口")
@RequestMapping("/chat/smart-reply")
@RestController
public class SmartReplyController {

    @Autowired
    private LLMService llmService;

    private static final String REPLY_PROMPT =
            "你是一名校园社交聊天场景的智能回复助手。请根据对方发来的消息内容，"
                    + "生成3条自然、得体、口语化的中文回复建议，每条不超过20个字，"
                    + "语气贴近朋友聊天，避免正式书面语。"
                    + "直接输出3条回复，每条占一行，不要编号、不要引号、不要多余解释。";

    @Operation(summary = "生成智能回复建议")
    @PostMapping
    public R<List<String>> smartReply(@RequestBody SmartReplyRequest request) {
        if (request == null || StringUtils.isBlank(request.getMessage())) {
            return R.fail("消息内容不能为空");
        }
        String input = "对方发来的消息：" + request.getMessage().trim();
        try {
            ChatContext chatContext = ChatContext.builder()
                    .input(input)
                    .stream(false)
                    .prompt(REPLY_PROMPT)
                    .build();
            Flux<String> flux = llmService.chat(chatContext);
            List<String> chunks = flux.collectList().block(Duration.ofSeconds(30));
            String text = chunks == null ? "" : String.join("", chunks);
            List<String> replies = parseReplies(text);
            if (replies.isEmpty()) {
                return R.ok(fallbackReplies(), "生成失败，已返回默认建议");
            }
            return R.ok(replies, "生成成功");
        } catch (Exception e) {
            log.error("生成智能回复失败", e);
            return R.ok(fallbackReplies(), "生成失败，已返回默认建议");
        }
    }

    /**
     * 解析模型输出为回复建议列表（最多3条）
     */
    private List<String> parseReplies(String text) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isBlank(text)) {
            return result;
        }
        // 优先按行拆分
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String cleaned = cleanLine(line);
            if (!cleaned.isEmpty() && !result.contains(cleaned)) {
                result.add(cleaned);
                if (result.size() >= 3) {
                    break;
                }
            }
        }
        // 行拆分不足时，按编号拆分（1. 2. 3.）
        if (result.size() < 3) {
            String[] parts = text.split("\\d+[.、．]");
            for (String part : parts) {
                String cleaned = cleanLine(part);
                if (!cleaned.isEmpty() && !result.contains(cleaned)) {
                    result.add(cleaned);
                    if (result.size() >= 3) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    private String cleanLine(String line) {
        if (line == null) {
            return "";
        }
        String s = line.trim()
                .replaceAll("^[\\s\\d\\[\\]()（）.、．:：\"'“”\\-]+", "")
                .replaceAll("[\\s\\[\\]()（）.、．:：\"'“”\\-]+$", "");
        if (s.length() > 30) {
            s = s.substring(0, 30);
        }
        return s.trim();
    }

    private List<String> fallbackReplies() {
        List<String> list = new ArrayList<>();
        list.add("好的，收到～");
        list.add("哈哈，确实是这样");
        list.add("嗯嗯，我明白你的意思");
        return list;
    }
}
