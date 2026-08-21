package com.edtech.platform.communication.service;

import com.edtech.platform.common.event.booking.BookingCreatedEvent;
import com.edtech.platform.common.event.payment.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ConversationEventListener {

    private final ConversationService conversationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        conversationService.getOrCreateConversation(event.getTeacherId(), event.getStudentId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCreated(BookingCreatedEvent event) {
        conversationService.getOrCreateConversation(event.getTeacherId(), event.getStudentId());
    }
}
