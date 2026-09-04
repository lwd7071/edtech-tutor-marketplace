package com.edtech.platform.communication.bridge;

import com.edtech.platform.common.event.booking.BookingCompletedEvent;
import com.edtech.platform.common.event.booking.BookingCreatedEvent;
import com.edtech.platform.common.event.finance.PayoutProcessedEvent;
import com.edtech.platform.common.event.finance.PayoutApprovedEvent;
import com.edtech.platform.common.event.payment.InvoicePaidEvent;
import com.edtech.platform.common.event.payment.PaymentSucceededEvent;
import com.edtech.platform.communication.dto.TransactionNotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventWebSocketBridge {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("Bridging BookingCreatedEvent to WebSocket for bookingId={}", event.getBookingId());
        TransactionNotificationMessage msg = new TransactionNotificationMessage(
                "BOOKING_CREATED",
                "Lịch học mới đã được đặt thành công",
                event.getBookingId()
        );
        messagingTemplate.convertAndSend("/topic/bookings." + event.getTeacherId(), msg);
        messagingTemplate.convertAndSend("/topic/bookings." + event.getStudentId(), msg);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCompleted(BookingCompletedEvent event) {
        log.info("Bridging BookingCompletedEvent to WebSocket for bookingId={}", event.getBookingId());
        TransactionNotificationMessage msg = new TransactionNotificationMessage(
                "BOOKING_COMPLETED",
                "Buổi học đã được hoàn thành và đối soát",
                event.getBookingId()
        );
        messagingTemplate.convertAndSend("/topic/bookings." + event.getStudentId(), msg);
        messagingTemplate.convertAndSend("/topic/wallet." + event.getTeacherId(), msg);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Bridging PaymentSucceededEvent to WebSocket for invoiceId={}", event.getInvoiceId());
        TransactionNotificationMessage msg = new TransactionNotificationMessage(
                "PAYMENT_SUCCEEDED",
                "Thanh toán gói học thành công",
                event.getInvoiceId()
        );
        messagingTemplate.convertAndSend("/topic/invoices." + event.getStudentId(), msg);
        messagingTemplate.convertAndSend("/topic/wallet." + event.getTeacherId(), msg);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInvoicePaid(InvoicePaidEvent event) {
        TransactionNotificationMessage msg = new TransactionNotificationMessage(
                "INVOICE_PAID", "Thanh toán hóa đơn thành công", event.getInvoiceId());
        messagingTemplate.convertAndSend("/topic/invoices." + event.getStudentId(), msg);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePayoutProcessed(PayoutProcessedEvent event) {
        log.info("Bridging PayoutProcessedEvent to WebSocket for payoutId={}", event.getPayoutRequestId());
        TransactionNotificationMessage msg = new TransactionNotificationMessage(
                "PAYOUT_PROCESSED",
                "Yêu cầu rút tiền đã được cập nhật trạng thái: " + event.getStatus(),
                event.getPayoutRequestId()
        );
        messagingTemplate.convertAndSend("/topic/wallet." + event.getTeacherId(), msg);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePayoutApproved(PayoutApprovedEvent event) {
        TransactionNotificationMessage msg = new TransactionNotificationMessage(
                "PAYOUT_APPROVED", "Yêu cầu rút tiền đã được duyệt", event.getPayoutRequestId());
        messagingTemplate.convertAndSend("/topic/wallet." + event.getTeacherId(), msg);
    }
}

