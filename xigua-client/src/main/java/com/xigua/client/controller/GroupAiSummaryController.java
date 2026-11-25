package com.xigua.client.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigua.api.service.ChatMessageService;
import com.xigua.api.service.GroupAIService;
import com.xigua.api.service.GroupService;
import com.xigua.client.mapper.GroupMemberMapper;
import com.xigua.common.core.base.R;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.UserContext;
import com.xigua.domain.dto.GetHistoryMes;
import com.xigua.domain.entity.Group;
import com.xigua.domain.entity.GroupMember;
import com.xigua.domain.vo.ChatMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 群聊 AI 摘要与待办
 */
@Slf4j
@Tag(name = "群聊 AI 摘要")
@RequestMapping("/group/ai")
@RestController
public class GroupAiSummaryController {

    private static final int MAX_MESSAGES = 50;
    private static final int MAX_TRANSCRIPT_CHARS = 5000;
    private static final int ROLE_OWNER = 1;
    private static final int ROLE_ADMIN = 2;
    private static final int AI_COOLDOWN_SECONDS = 8;
    private static final String AI_COOLDOWN_KEY_PREFIX = "group_ai_summary:cooldown:";

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private GroupService groupService;

    @DubboReference(check = false, timeout = 3000)
    private ChatMessageService chatMessageService;

    @DubboReference(check = false, timeout = 60000)
    private GroupAIService groupAIService;

    @Operation(summary = "生成群聊摘要与待办")
    @PostMapping("/summary")
    public R<Map<String, Object>> summary(@RequestBody Map<String, Object> body) {
        String groupId = body == null ? null : String.valueOf(body.getOrDefault("groupId", ""));
        if (StringUtils.isBlank(groupId) || "null".equals(groupId)) {
            return R.fail("群id不能为空");
        }
        String userId = currentUserId();
        GroupMember member = findMember(groupId, userId);
        if (member == null) {
            return R.fail("你不在该群聊中");
        }
        Integer role = member.getRole();
        if (role == null || (role != ROLE_OWNER && role != ROLE_ADMIN)) {
            return R.fail("仅群主或管理员可生成群摘要");
        }

        GetHistoryMes req = new GetHistoryMes();
        req.setSenderId(userId);
        req.setReceiverId(groupId);
        req.setChatType(2);
        req.setPageNum(1);
        req.setPageSize(Math.min(MAX_MESSAGES, 50));
        List<ChatMessageVO> messages;
        try {
            messages = chatMessageService.getHistoryMes(req).getRows();
        } catch (Exception e) {
            log.error("读取群历史消息失败 groupId={}", groupId, e);
            return R.fail("群消息读取失败，请稍后重试");
        }
        if (messages == null || messages.isEmpty()) {
            return R.fail("这个群还没有可总结的消息");
        }

        String groupName = "群聊";
        try {
            Group group = groupService.getById(groupId);
            if (group != null && StringUtils.isNotBlank(group.getGroupName())) {
                groupName = group.getGroupName();
            }
        } catch (Exception e) {
            log.warn("读取群名称失败 groupId={}", groupId, e);
        }

        String transcript = buildTranscript(messages);
        if (StringUtils.isBlank(transcript)) {
            return R.fail("这个群还没有可总结的文字消息");
        }

        String cooldownKey = AI_COOLDOWN_KEY_PREFIX + groupId + ":" + userId;
        if (!acquireAiCooldown(cooldownKey)) {
            return R.fail("AI 摘要生成太频繁，请 " + aiCooldownRemaining(cooldownKey) + " 秒后再试");
        }

        String raw = groupAIService.summarize(groupName, transcript);
        Map<String, Object> summary = parseSummary(raw, transcript);
        summary.put("groupId", groupId);
        summary.put("groupName", groupName);
        summary.put("messageCount", messages.size());
        summary.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return R.ok(summary, "摘要已生成");
    }

    private String buildTranscript(List<ChatMessageVO> messages) {
        StringBuilder sb = new StringBuilder();
        int used = 0;
        for (ChatMessageVO message : messages) {
            if (message == null) continue;
            if (message.getWithdrawn() != null && message.getWithdrawn() == 1) continue;
            String content = message.getMessage();
            Integer type = message.getMessageType();
            if (type != null && type != 1) {
                content = switch (type) {
                    case 2 -> "[图片]";
                    case 3 -> "[语音]";
                    case 4 -> "[视频]";
                    case 5 -> "[文件] " + safeFileName(content);
                    default -> "[消息]";
                };
            }
            if (StringUtils.isBlank(content)) continue;
            if (content.length() > 200) {
                content = content.substring(0, 200) + "…";
            }
            String sender = message.getSender() == null ? null : message.getSender().getUsername();
            if (StringUtils.isBlank(sender)) sender = "成员";
            String line = "[" + StringUtils.defaultIfBlank(message.getCreateTime(), "") + "] " + sender + "：" + content + "\n";
            if (used + line.length() > MAX_TRANSCRIPT_CHARS) {
                break;
            }
            sb.append(line);
            used += line.length();
        }
        return sb.toString();
    }

    private String safeFileName(String url) {
        if (StringUtils.isBlank(url)) return "";
        String name = url.substring(url.lastIndexOf('/') + 1);
        int idx = name.indexOf('_');
        return idx >= 0 && idx + 1 < name.length() ? name.substring(idx + 1) : name;
    }

    /**
     * 模型通常返回纯 JSON，但偶尔会包一层 Markdown 代码块；这里统一提取第一个完整对象。
     */
    private Map<String, Object> parseSummary(String raw, String transcript) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "");
        result.put("todos", new ArrayList<>());
        if (StringUtils.isBlank(raw)) {
            result.put("summary", "AI 暂时没有返回摘要，请稍后重试。");
            return result;
        }
        String cleaned = raw.trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }
        try {
            JSONObject json = JSON.parseObject(cleaned);
            String summary = json.getString("summary");
            if (StringUtils.isNotBlank(summary)) {
                result.put("summary", summary.trim());
            }
            JSONArray todos = json.getJSONArray("todos");
            if (todos != null) {
                List<Map<String, String>> list = new ArrayList<>();
                for (int i = 0; i < todos.size() && i < 5; i++) {
                    Object item = todos.get(i);
                    Map<String, String> todo = new LinkedHashMap<>();
                    if (item instanceof JSONObject obj) {
                        todo.put("content", obj.getString("content"));
                        todo.put("owner", StringUtils.defaultIfBlank(obj.getString("owner"), "待定"));
                        todo.put("due", StringUtils.defaultIfBlank(obj.getString("due"), "未指定"));
                    } else {
                        todo.put("content", String.valueOf(item));
                        todo.put("owner", "待定");
                        todo.put("due", "未指定");
                    }
                    if (StringUtils.isNotBlank(todo.get("content")) && !"null".equals(todo.get("content"))) {
                        list.add(todo);
                    }
                }
                result.put("todos", list);
            }
        } catch (Exception e) {
            log.warn("解析 AI 摘要 JSON 失败，按纯文本返回：{}", e.getMessage());
            result.put("summary", raw.trim());
        }
        if (StringUtils.isBlank(String.valueOf(result.get("summary")))) {
            result.put("summary", "AI 没有生成有效摘要，请重试。");
        }
        return result;
    }

    private GroupMember findMember(String groupId, String userId) {
        return groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getStatus, 1)
                .eq(GroupMember::getDelFlag, 0)
                .last("limit 1"));
    }

    /**
     * 用 Redis 原子写入做服务端频控，避免同一用户/同一群连续触发模型调用。
     * Redis 不可用时放行本次请求，优先保证摘要功能可用。
     */
    private boolean acquireAiCooldown(String key) {
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, "1", Duration.ofSeconds(AI_COOLDOWN_SECONDS));
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("AI 摘要频控写入 Redis 失败，本次放行 key={}", key, e);
            return true;
        }
    }

    private long aiCooldownRemaining(String key) {
        try {
            Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null || ttl < 0) {
                return AI_COOLDOWN_SECONDS;
            }
            return Math.max(1, ttl);
        } catch (Exception e) {
            log.warn("AI 摘要频控读取剩余时间失败 key={}", key, e);
            return 1;
        }
    }

    private String currentUserId() {
        if (UserContext.get() == null || StringUtils.isEmpty(UserContext.get().getUserId())) {
            throw new BusinessException("请先登录");
        }
        return UserContext.get().getUserId();
    }
}
