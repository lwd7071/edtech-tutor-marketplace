package com.edtech.platform.communication.bridge;

import com.edtech.platform.common.event.booking.BookingCompletedEvent;
import com.edtech.platform.common.event.booking.BookingCreatedEvent;
import com.edtech.platform.common.event.finance.PayoutProcessedEvent;
import com.edtech.platform.common.event.payment.PaymentSucceededEvent;
import com.edtech.platform.communication.dto.TransactionNotificationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 9.2 - TransactionEventWebSocketBridge Unit Test")
class TransactionEventWebSocketBridgeTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock private com.edtech.platform.teacher.facade.TeacherFacade teachers;
    private final UUID teacherAccountId = UUID.randomUUID();
    private TransactionEventWebSocketBridge bridge;

    @BeforeEach
    void setUp() {
        bridge = new TransactionEventWebSocketBridge(messagingTemplate, teachers);
        org.mockito.Mockito.when(teachers.getTeacher(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> new com.edtech.platform.teacher.facade.dto.TeacherSnapshot(inv.getArgument(0), teacherAccountId, "APPROVED", true, true, "Teacher", null, null, 1, true, false, java.util.List.of(), null, null));
    }

    @Test
    @DisplayName("BookingCreatedEvent should broadcast to teacher and student topic")
    void handleBookingCreated_shouldBroadcastToBothTopics() {
        UUID bookingId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        BookingCreatedEvent event = new BookingCreatedEvent(
                bookingId, studentId, teacherId, subjectId, "Math",
                Instant.now(), Instant.now().plusSeconds(3600)
        );

        bridge.handleBookingCreated(event);

        ArgumentCaptor<TransactionNotificationMessage> msgCaptor = ArgumentCaptor.forClass(TransactionNotificationMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/bookings." + teacherAccountId), msgCaptor.capture());
        verify(messagingTemplate).convertAndSend(eq("/topic/bookings." + studentId), msgCaptor.capture());

        assertThat(msgCaptor.getValue().getEventType()).isEqualTo("BOOKING_CREATED");
        assertThat(msgCaptor.getValue().getReferenceId()).isEqualTo(bookingId);
    }

    @Test
    @DisplayName("BookingCompletedEvent should broadcast to student booking and teacher wallet topic")
    void handleBookingCompleted_shouldBroadcastToStudentAndWallet() {
        UUID bookingId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        BookingCompletedEvent event = new BookingCompletedEvent(bookingId, studentId, teacherId, "Physics");

        bridge.handleBookingCompleted(event);

        ArgumentCaptor<TransactionNotificationMessage> msgCaptor = ArgumentCaptor.forClass(TransactionNotificationMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/bookings." + studentId), msgCaptor.capture());
        verify(messagingTemplate).convertAndSend(eq("/topic/wallet." + teacherAccountId), msgCaptor.capture());

        assertThat(msgCaptor.getValue().getEventType()).isEqualTo("BOOKING_COMPLETED");
        assertThat(msgCaptor.getValue().getReferenceId()).isEqualTo(bookingId);
    }

    @Test
    @DisplayName("PaymentSucceededEvent should broadcast to student invoice and teacher wallet topic")
    void handlePaymentSucceeded_shouldBroadcastToInvoiceAndWallet() {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                studentId, teacherId, invoiceId, packageId, "Package 10 Sessions", 1000000L
        );

        bridge.handlePaymentSucceeded(event);

        ArgumentCaptor<TransactionNotificationMessage> msgCaptor = ArgumentCaptor.forClass(TransactionNotificationMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/invoices." + studentId), msgCaptor.capture());
        verify(messagingTemplate).convertAndSend(eq("/topic/wallet." + teacherAccountId), msgCaptor.capture());

        assertThat(msgCaptor.getValue().getEventType()).isEqualTo("PAYMENT_SUCCEEDED");
        assertThat(msgCaptor.getValue().getReferenceId()).isEqualTo(invoiceId);
    }

    @Test
    @DisplayName("PayoutProcessedEvent should broadcast to teacher wallet topic")
    void handlePayoutProcessed_shouldBroadcastToWallet() {
        UUID payoutId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        PayoutProcessedEvent event = new PayoutProcessedEvent(payoutId, teacherId, 500000L, "SUCCEEDED");

        bridge.handlePayoutProcessed(event);

        ArgumentCaptor<TransactionNotificationMessage> msgCaptor = ArgumentCaptor.forClass(TransactionNotificationMessage.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/wallet." + teacherAccountId), msgCaptor.capture());

        assertThat(msgCaptor.getValue().getEventType()).isEqualTo("PAYOUT_PROCESSED");
        assertThat(msgCaptor.getValue().getReferenceId()).isEqualTo(payoutId);
    }
}


