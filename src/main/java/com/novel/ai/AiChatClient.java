package com.novel.ai;

public interface AiChatClient {
    String complete(String systemPrompt, String userPrompt);
}
