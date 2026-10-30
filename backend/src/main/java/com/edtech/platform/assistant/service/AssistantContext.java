package com.edtech.platform.assistant.service;

import com.edtech.platform.assistant.dto.AssistantSource;

import java.util.List;

public record AssistantContext(String evidence, List<AssistantSource> sources, String directReply) {
    public AssistantContext {
        sources = List.copyOf(sources);
    }

    public AssistantContext(String evidence, List<AssistantSource> sources) {
        this(evidence, sources, null);
    }
}
