package com.xigua.ai.controller;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.ai.client.CogViewClient;
import com.xigua.ai.domain.dto.ChatRequest;
import com.xigua.ai.domain.dto.ImageRequest;
import com.xigua.api.service.AiMessageService;
import com.xigua.api.service.AiSessionService;
import com.xigua.api.service.FileService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.dto.CreateSessionRequest;
import com.xigua.domain.dto.MultipartFileDTO;
import com.xigua.domain.vo.AiHistoryMessageVO;
import com.xigua.ai.service.AIService;
import com.xigua.common.core.base.R;
import com.xigua.domain.dto.GetAiHistoryMes;
import com.xigua.domain.dto.PageSessionsDTO;
import com.xigua.domain.entity.AiSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * @ClassName ChatController
 * @Description
 * @Author wangjinfei
 * @Date 2026/2/24 19:47
 */
@Tag(name = "ai聊天接口")
@RequestMapping("/chat/session")
@RestController
public class SessionController {
    @Autowired
    private AIService aiService;
    @Autowired
    private AiSessionService aiSessionService;
    @Autowired
    private AiMessageService aiMessageService;
    @Autowired
    private CogViewClient cogViewClient;
    @DubboReference
    private FileService fileService;

    /**
     * AI绘画（文生图）：生成图片并上传MinIO，图片消息以 "img:" 前缀写入会话
     */
    @Operation(summary = "AI绘画")
    @PostMapping("/image")
    public R<Map<String, String>> generateImage(@RequestBody ImageRequest request) {
        if (request == null || StringUtils.isEmpty(request.getPrompt())) {
            throw new BusinessException("绘画描述不能为空");
        }
        if (StringUtils.isEmpty(request.getUserId())) {
            throw new BusinessException("用户id不能为空");
        }
        if (StringUtils.isEmpty(request.getBotId())) {
            throw new BusinessException("机器人id不能为空");
        }
        // 会话归属只认当前登录人
        request.setUserId(currentUserId());

        // 1. 调用智谱 CogView 生成图片
        JSONObject result = cogViewClient.generate(request.getPrompt());
        String remoteUrl = result.getJSONArray("data").getJSONObject(0).getString("url");
        if (StringUtils.isEmpty(remoteUrl)) {
            throw new BusinessException("AI 绘图未返回图片地址");
        }

        // 2. 下载图片字节
        byte[] bytes = downloadImage(remoteUrl);

        // 3. 上传 MinIO
        MultipartFileDTO file = new MultipartFileDTO();
        file.setOriginalFilename("ai_" + System.currentTimeMillis() + ".png");
        file.setContentType("image/png");
        file.setSize((long) bytes.length);
        file.setBytes(bytes);
        String url = fileService.upload(file);
        if (StringUtils.isEmpty(url) || "上传失败".equals(url)) {
            throw new BusinessException("图片上传失败");
        }

        // 4. 未传会话则自动创建（复用现有会话创建逻辑）
        String sessionId = request.getSessionId();
        if (StringUtils.isEmpty(sessionId)) {
            CreateSessionRequest cs = new CreateSessionRequest();
            cs.setUserId(request.getUserId());
            cs.setBotId(request.getBotId());
            cs.setInput(request.getPrompt());
            AiSession session = aiSessionService.createSession(cs);
            sessionId = session.getId();
        } else {
            // 指定会话时必须属于当前登录人，防止把图片写进别人的会话
            aiSessionService.getOwnedSession(sessionId);
        }

        // 5. 图片消息挂入会话（img: 前缀，前端据此渲染图片气泡）
        aiMessageService.saveAiMessage(sessionId, "img:" + url);

        Map<String, String> resp = new HashMap<>();
        resp.put("sessionId", sessionId);
        resp.put("url", url);
        return R.ok(resp);
    }

    private byte[] downloadImage(String url) {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new BusinessException("图片下载失败: HTTP " + response.code());
            }
            return response.body().bytes();
        } catch (IOException e) {
            throw new BusinessException("图片下载失败: " + e.getMessage());
        }
    }

    /**
     * 创建会话
     */
    @Operation(summary = "创建会话")
    @PostMapping("/create")
    public R<AiSession> createSession(@RequestBody CreateSessionRequest request){
        request.setUserId(currentUserId());
        return R.ok(aiSessionService.createSession(request));
    }


    /**
     * 发送消息
     */
    @Operation(summary = "发送消息")
    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatRequest request){
        // 先校验会话归属，避免把消息写进别人的 AI 会话
        aiSessionService.getOwnedSession(request.getSessionId());
        return aiService.chatSession(request);
    }


    /**
     * 查询历史消息
     */
    @Operation(summary = "查询历史消息")
    @PostMapping("/messages")
    public R<BasePageVO<AiHistoryMessageVO>> history(@RequestBody GetAiHistoryMes dto){
        aiSessionService.getOwnedSession(dto.getSessionId());
        return R.ok(aiMessageService.pageMessages(dto));
    }


    /**
     * 删除会话
     */
    @Operation(summary = "删除会话")
    @DeleteMapping("/{sessionId}")
    public R deleteSession(@PathVariable String sessionId){
        return R.ok(aiSessionService.deleteSession(sessionId));
    }

    /**
     * 获取会话
     */
    @Operation(summary = "获取会话")
    @PostMapping("/{botId}")
    public R<BasePageVO<AiSession>> pageSessions(@PathVariable String botId, @RequestBody PageSessionsDTO pageSessionsDTO){
        pageSessionsDTO.setBotId(botId);
        return R.ok(aiSessionService.pageSessions(pageSessionsDTO));
    }

    /**
     * 重命名会话
     */
    @Operation(summary = "重命名会话")
    @PostMapping("/{sessionId}/rename")
    public R renameSession(@PathVariable String sessionId, @RequestParam String title){
        return R.ok(aiSessionService.renameSession(sessionId, title));
    }

    /**
     * 切换会话模型（供应商）
     */
    @Operation(summary = "切换会话模型")
    @PostMapping("/{sessionId}/model")
    public R updateModel(@PathVariable String sessionId, @RequestParam String model){
        return R.ok(aiSessionService.updateModel(sessionId, model));
    }

    private String currentUserId() {
        if (UserContext.get() == null || StringUtils.isEmpty(UserContext.get().getUserId())) {
            throw new BusinessException("请先登录");
        }
        return UserContext.get().getUserId();
    }
}
