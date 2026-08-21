package com.edtech.platform.communication.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatReadRequest {
    @NotNull
    private UUID conversationId;
}
