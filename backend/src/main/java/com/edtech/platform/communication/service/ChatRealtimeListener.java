package com.edtech.platform.communication.service;

import com.edtech.platform.communication.event.ChatMessageCommitted;
import com.edtech.platform.communication.event.ChatMessagesRead;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatRealtimeListener {
    private final SimpMessagingTemplate messaging;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessage(ChatMessageCommitted event) {
        messaging.convertAndSendToUser(event.receiverId().toString(), "/queue/messages", event.message());
        messaging.convertAndSendToUser(event.senderId().toString(), "/queue/messages", event.message());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRead(ChatMessagesRead event) {
        messaging.convertAndSendToUser(
                event.recipientId().toString(), "/queue/messages/seen", event.conversationId());
    }
}
