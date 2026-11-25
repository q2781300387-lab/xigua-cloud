package com.xigua.ai.client;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * 智谱 CogView 文生图客户端（open.bigmodel.cn）
 * model: cogview-3-flash（免费）
 */
@Slf4j
public class CogViewClient {
    private static final String BASE_URL = "https://open.bigmodel.cn/api/paas/v4/images/generations";
    private String apiKey;
    private String model;

    public JSONObject generate(String prompt) {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("prompt", prompt);
        body.put("size", "1024x1024");
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                body.toJSONString());
        Request request = new Request.Builder()
                .url(BASE_URL)
                .method("POST", requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseStr = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                log.error("CogView request failed: code={}, body={}", response.code(), responseStr);
                throw new RuntimeException("AI 绘图服务调用失败: HTTP " + response.code());
            }
            JSONObject result = JSONObject.parseObject(responseStr);
            if (result == null || result.getJSONArray("data") == null
                    || result.getJSONArray("data").isEmpty()) {
                log.error("CogView empty result: {}", responseStr);
                throw new RuntimeException("AI 绘图未返回结果");
            }
            return result;
        } catch (IOException e) {
            log.error("CogView request error", e);
            throw new RuntimeException("AI 绘图请求异常: " + e.getMessage());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String apiKey;
        private String model;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public CogViewClient build() {
            CogViewClient client = new CogViewClient();
            if (StringUtils.isNotBlank(this.apiKey)) {
                client.apiKey = this.apiKey;
            }
            client.model = StringUtils.isNotBlank(this.model) ? this.model : "cogview-3-flash";
            return client;
        }
    }
}
