package com.edtech.platform.communication.event;

import com.edtech.platform.communication.dto.chat.MessageView;

import java.util.UUID;

public record ChatMessageCommitted(UUID senderId, UUID receiverId, MessageView message) {
}
