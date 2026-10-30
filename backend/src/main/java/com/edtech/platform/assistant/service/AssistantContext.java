package com.edtech.platform.assistant.service;

import com.edtech.platform.assistant.dto.AssistantSource;

import java.util.List;

public record AssistantContext(String evidence, List<AssistantSource> sources) {
    public AssistantContext {
        sources = List.copyOf(sources);
    }
}
