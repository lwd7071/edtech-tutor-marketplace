package com.edtech.platform.assistant.service;

import java.util.List;

public record AssistantKnowledgeArticle(
        String title,
        String content,
        List<String> aliases,
        String sourcePath,
        int sortOrder
) {
    public AssistantKnowledgeArticle {
        aliases = List.copyOf(aliases);
    }
}
