package com.edtech.platform.assistant.dto;

import java.util.List;

public record AssistantChatResponse(
        AssistantOutcome outcome,
        String reply,
        List<AssistantSource> sources,
        String language
) {
    public AssistantChatResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
