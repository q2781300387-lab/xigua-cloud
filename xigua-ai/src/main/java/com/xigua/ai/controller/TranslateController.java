package com.xigua.ai.controller;

import com.xigua.ai.domain.dto.TranslateRequest;
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

/**
 * @ClassName TranslateController
 * @Description 消息翻译：把聊天内容翻译成中文 / 英文 / 日文，只返回译文
 * @Author xigua
 * @Date 2026/11/13 16:00
 */
@Slf4j
@Tag(name = "消息翻译接口")
@RequestMapping("/chat/translate")
@RestController
public class TranslateController {

    @Autowired
    private LLMService llmService;

    private static final String PROMPT =
            "你是一名专业翻译引擎。请把用户给出的原文翻译成指定的目标语言，"
                    + "只输出译文本身，不要解释、不要加引号、不要标注语言名称、不要保留原文。"
                    + "保留原文的语气、换行和专有名词，口语内容按口语翻译。";

    private static final int MAX_LENGTH = 2000;

    @Operation(summary = "翻译消息")
    @PostMapping
    public R<String> translate(@RequestBody TranslateRequest request) {
        if (request == null || StringUtils.isBlank(request.getText())) {
            return R.fail("待翻译内容不能为空");
        }
        String text = request.getText().trim();
        if (text.length() > MAX_LENGTH) {
            return R.fail("单条消息最多翻译 " + MAX_LENGTH + " 字");
        }
        String target = normalizeTarget(request.getTarget());
        String input = "目标语言：" + target + "\n原文：\n" + text;
        try {
            ChatContext chatContext = ChatContext.builder()
                    .input(input)
                    .stream(false)
                    .prompt(PROMPT)
                    .build();
            Flux<String> flux = llmService.chat(chatContext);
            List<String> chunks = flux.collectList().block(Duration.ofSeconds(30));
            String result = chunks == null ? "" : String.join("", chunks).trim();
            result = stripQuotes(result);
            if (StringUtils.isBlank(result)) {
                return R.fail("翻译失败，请稍后重试");
            }
            return R.ok(result, "翻译成功");
        } catch (Exception e) {
            log.error("翻译消息失败：{}", e.getMessage(), e);
            return R.fail("翻译失败，请稍后重试");
        }
    }

    private String normalizeTarget(String target) {
        if (StringUtils.isBlank(target)) {
            return "中文";
        }
        String t = target.trim().toLowerCase();
        if ("en".equals(t) || "english".equals(t)) {
            return "英文";
        }
        if ("ja".equals(t) || "jp".equals(t) || "japanese".equals(t)) {
            return "日文";
        }
        return "中文";
    }

    private String stripQuotes(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String s = text.trim();
        if (s.length() >= 2) {
            if ((s.startsWith("\"") && s.endsWith("\""))
                    || (s.startsWith("“") && s.endsWith("”"))
                    || (s.startsWith("'") && s.endsWith("'"))) {
                s = s.substring(1, s.length() - 1).trim();
            }
        }
        return s;
    }
}
