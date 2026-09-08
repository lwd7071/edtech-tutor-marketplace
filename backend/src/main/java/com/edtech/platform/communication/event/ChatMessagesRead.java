package com.edtech.platform.communication.event;

import java.util.UUID;

public record ChatMessagesRead(UUID recipientId, UUID conversationId) {
}
