package com.novel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiContentParser {
    private final ObjectMapper mapper = new ObjectMapper();

    public record Plan(String proposal, String outline) {}

    public List<String> questions(String content) {
        JsonNode items = json(content).path("questions");
        if (!items.isArray() || items.isEmpty()) throw new AiServiceException("AI 未返回有效问题，请重试");
        List<String> result = new ArrayList<>();
        for (JsonNode item : items) {
            if (!item.isTextual() || item.asText().isBlank()) throw new AiServiceException("AI 问题格式无效，请重试");
            result.add(item.asText().trim());
            if (result.size() == 3) break;
        }
        return result;
    }

    public Plan plan(String content) {
        JsonNode root = json(content);
        JsonNode proposal = root.path("proposal");
        JsonNode outline = root.path("outline");
        if (!proposal.isTextual() || proposal.asText().isBlank() || !outline.isTextual() || outline.asText().isBlank()) {
            throw new AiServiceException("AI 未返回完整的故事方案和大纲，请重试");
        }
        return new Plan(proposal.asText().trim(), outline.asText().trim());
    }

    private JsonNode json(String content) {
        if (content == null || content.isBlank()) throw new AiServiceException("AI 返回空内容，请重试");
        String value = content.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        try { return mapper.readTree(value); }
        catch (Exception error) { throw new AiServiceException("AI 返回格式无效，请重试"); }
    }
}
