package com.edtech.platform.assistant.provider;

import com.edtech.platform.assistant.dto.AssistantHistoryMessage;

import java.util.List;

public interface GroqChatCompletionPort {
    String complete(String systemPrompt, List<AssistantHistoryMessage> history, String message);
}
