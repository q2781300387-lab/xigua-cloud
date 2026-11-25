package com.xigua.ai.service;

import com.xigua.ai.llm.LLMService;
import com.xigua.ai.llm.model.ChatContext;
import com.xigua.api.service.GroupAIService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * @ClassName GroupAIServiceImpl
 * @Description 群聊 AI 小助手实现：按机器人人格调用 LLM 生成回复
 * @Author xigua
 * @Date 2026/11/3 15:30
 */
@Slf4j
@DubboService(timeout = 30000)
public class GroupAIServiceImpl implements GroupAIService {

    private static final String BOT_XIAOGUA = "243400000000000001";
    private static final String BOT_LAOXUEJIU = "243400000000000002";

    private static final String PROMPT_XIAOGUA =
            "你是DeepSeek·小瓜，热情话痨、氛围担当的校园群聊AI小助手。你说话亲切活泼，"
                    + "喜欢用“嘿嘿”“绝了”“你品品”这类口头禅，把群友当老朋友。用简体中文简洁回答群聊里的提问，150字以内。";

    private static final String PROMPT_LAOXUEJIU =
            "你是DeepSeek·老学究，理性严谨、学术风格的校园群聊AI小助手。回答讲究条理，"
                    + "习惯用“其一”“其二”“结论是”“存疑”等结构词，遇到不确定明确说明，不编造数据。用简体中文简洁回答，150字以内。";

    private static final String PROMPT_SUMMARY =
            "你是中文群聊纪要助手。根据群聊记录提炼结论和待办，只依据记录中的事实，不补造人物、时间、金额或结论。"
                    + "必须只输出一个 JSON 对象，不要 Markdown 代码块，不要前后说明。格式："
                    + "{\"summary\":\"100-180字的中文摘要\",\"todos\":[{\"content\":\"待办事项\",\"owner\":\"明确被指派的人，没有则写待定\",\"due\":\"明确时间，没有则写未指定\"}]}。"
                    + "todos 最多 5 条；没有明确待办时输出空数组。";

    @Autowired
    private LLMService llmService;

    @Override
    public String summarize(String groupName, String transcript) {
        String input = "群名称：" + (groupName == null ? "未命名群聊" : groupName)
                + "\n群聊记录（按时间正序）：\n" + (transcript == null ? "" : transcript);
        log.info("[GroupAI] summarize enter: groupName={}, transcriptLen={}", groupName,
                transcript == null ? 0 : transcript.length());
        try {
            ChatContext chatContext = ChatContext.builder()
                    .input(input)
                    .stream(false)
                    .prompt(PROMPT_SUMMARY)
                    .build();
            Flux<String> flux = llmService.chat(chatContext);
            List<String> chunks = flux.collectList().block(Duration.ofSeconds(45));
            String text = chunks == null ? "" : String.join("", chunks).trim();
            log.info("[GroupAI] summarize llm returned: len={}", text.length());
            return text;
        } catch (Exception e) {
            log.error("群聊摘要生成失败, groupName={}", groupName, e);
            return "";
        }
    }
    @Override
    public String chat(String botId, String question) {
        String prompt = BOT_LAOXUEJIU.equals(botId) ? PROMPT_LAOXUEJIU : PROMPT_XIAOGUA;
        log.info("[GroupAI] chat enter: botId={}, question={}", botId, question);
        try {
            ChatContext chatContext = ChatContext.builder()
                    .input(question == null ? "" : question)
                    .stream(false)
                    .prompt(prompt)
                    .build();
            Flux<String> flux = llmService.chat(chatContext);
            List<String> chunks = flux.collectList().block(Duration.ofSeconds(30));
            String text = chunks == null ? "" : String.join("", chunks).trim();
            log.info("[GroupAI] chat llm returned: len={}, preview={}", text.length(),
                    text.length() > 50 ? text.substring(0, 50) : text);
            if (text.isEmpty()) {
                return "（AI 小助手暂时开小差了，稍后再试哦）";
            }
            return text;
        } catch (Exception e) {
            log.error("群聊AI小助手回复失败, botId={}, question={}", botId, question, e);
            return "（AI 小助手暂时开小差了，稍后再试哦）";
        }
    }
}
