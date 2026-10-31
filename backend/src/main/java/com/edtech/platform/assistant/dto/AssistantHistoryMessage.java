package com.edtech.platform.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssistantHistoryMessage(
        @NotNull Role role,
        @NotBlank @Size(max = 1000) String content
) {
    public enum Role {
        USER,
        ASSISTANT
    }
}
