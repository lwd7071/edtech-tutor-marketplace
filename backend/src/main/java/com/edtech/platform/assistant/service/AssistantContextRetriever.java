package com.edtech.platform.assistant.service;

import java.util.Optional;

public interface AssistantContextRetriever {
    Optional<AssistantContext> retrieve(String question);
}
