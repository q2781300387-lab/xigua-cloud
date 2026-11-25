package com.xigua.ai.helper;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.ai.domain.enums.Prompt;
import com.xigua.ai.domain.enums.RoleType;
import com.xigua.ai.llm.model.ChatContext;
import com.xigua.ai.llm.LLMService;
import com.xigua.ai.llm.model.Memory;
import com.xigua.ai.utils.PromptUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName IntentRecognizer
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/10/18 19:38
 */
@Slf4j
@Component
public class IntentRecognizer {
    @Autowired
    private LLMService llmService;

    public IntentType detect(String input){
        ChatContext chatContext = ChatContext.builder()
                .prompt(PromptUtil.getPrompt(Prompt.DETECT_INTENT))
                .input(input)
                .stream(false)
                .build();
        try {
            Flux<String> output = llmService.chat(chatContext);
            IntentType intentType = parseIntent(output.blockFirst());
            if(intentType != null){
                return intentType;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return IntentType.UNKNOWN;
    }

    private IntentType parseIntent(String output){
        IntentType intentType = IntentType.UNKNOWN;
        try {
            if (StringUtils.isEmpty(output)) {
                return intentType;
            }
            JSONObject intentJson = JSONObject.parseObject(output);
            String intent = intentJson.getString("intent");
            if (StringUtils.isNotEmpty(intent)) {
                intentType = IntentType.fromType(intent);
                if (intentType != null) {
                    return intentType;
                }
            }
        } catch (Exception e) {
            log.error("解析意图失败, 输出内容: {}", output, e);
        }
        return intentType;
    }
}
