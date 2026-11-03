package com.novel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novel.ai.AiSettingsService.ResolvedSettings;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleClient implements AiChatClient {
    private final AiSettingsService settings;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiCompatibleClient(AiSettingsService settings) { this.settings = settings; }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        ResolvedSettings config = settings.resolved();
        try {
            String payload = mapper.writeValueAsString(Map.of(
                    "model", config.model(),
                    "messages", List.of(Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt))));
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiServiceException("AI 服务请求失败（HTTP " + response.statusCode() + "），请检查地址、模型和密钥");
            }
            JsonNode content = mapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) throw new AiServiceException("AI 返回空内容，请重试");
            return content.asText();
        } catch (AiServiceException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("AI 请求已中断", error);
        } catch (Exception error) {
            throw new AiServiceException("无法连接 AI 服务，请检查地址和网络", error);
        }
    }
}
