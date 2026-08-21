package com.edtech.platform.communication.service;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.auth.service.EmailService;
import com.edtech.platform.common.event.booking.BookingCompletedEvent;
import com.edtech.platform.common.event.booking.BookingCreatedEvent;
import com.edtech.platform.common.event.payment.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSucceededEvent(PaymentSucceededEvent event) {
        log.info("Handling PaymentSucceededEvent for invoice {}", event.getInvoiceId());

        String title = "Thanh toán thành công";
        String content = String.format("Thanh toán thành công cho gói %s - %d VND", event.getPackageName(), event.getAmountVnd());

        // Notify Student
        notificationService.createNotification(
                event.getStudentId(),
                "PAYMENT_SUCCEEDED",
                title,
                content,
                "INVOICE",
                event.getInvoiceId()
        );

        // Notify Teacher
        notificationService.createNotification(
                event.getTeacherId(),
                "PAYMENT_SUCCEEDED",
                title,
                content,
                "INVOICE",
                event.getInvoiceId()
        );

        // Send Email to Parent if needed
        userRepository.findById(event.getStudentId()).ifPresent(student -> {
            if (Boolean.TRUE.equals(student.getNotifyParent()) && student.getParentEmail() != null) {
                emailService.sendNotificationEmail(student.getParentEmail(), title, content);
            }
        });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCreatedEvent(BookingCreatedEvent event) {
        log.info("Handling BookingCreatedEvent for booking {}", event.getBookingId());

        String title = "Lịch học mới";
        String content = String.format("Bạn có một lịch học mới môn %s vào lúc %s", event.getSubjectName(), event.getStartTime().toString());

        // Notify Student
        notificationService.createNotification(
                event.getStudentId(),
                "BOOKING_CREATED",
                title,
                content,
                "BOOKING",
                event.getBookingId()
        );

        // Send Email to Student
        userRepository.findById(event.getStudentId()).ifPresent(student -> {
            if (student.getEmail() != null) {
                emailService.sendNotificationEmail(student.getEmail(), title, content);
            }
        });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCompletedEvent(BookingCompletedEvent event) {
        log.info("Handling BookingCompletedEvent for booking {}", event.getBookingId());

        String title = "Hoàn thành buổi học";
        String content = String.format("Buổi học môn %s đã hoàn thành", event.getSubjectName());

        // Notify Student
        notificationService.createNotification(
                event.getStudentId(),
                "BOOKING_COMPLETED",
                title,
                content,
                "BOOKING",
                event.getBookingId()
        );

        // Send Email to Student and Parent
        userRepository.findById(event.getStudentId()).ifPresent(student -> {
            if (student.getEmail() != null) {
                emailService.sendNotificationEmail(student.getEmail(), title, content);
            }
            if (Boolean.TRUE.equals(student.getNotifyParent()) && student.getParentEmail() != null) {
                emailService.sendNotificationEmail(student.getParentEmail(), title, content);
            }
        });
    }
}
