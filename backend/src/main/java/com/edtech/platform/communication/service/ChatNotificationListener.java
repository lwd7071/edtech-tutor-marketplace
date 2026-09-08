package com.edtech.platform.communication.service;

import com.edtech.platform.communication.event.ChatMessageCommitted;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatNotificationListener {
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessage(ChatMessageCommitted event) {
        notifications.createNotification(
                event.receiverId(),
                "NEW_MESSAGE",
                "Bạn có tin nhắn mới",
                "Bạn vừa nhận được một tin nhắn mới",
                "MESSAGE",
                event.message().getId());
    }
}
