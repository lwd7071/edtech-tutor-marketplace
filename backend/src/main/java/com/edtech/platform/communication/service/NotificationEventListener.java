package com.edtech.platform.communication.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.mail.MailService;
import com.edtech.platform.common.event.booking.BookingCompletedEvent;
import com.edtech.platform.common.event.booking.BookingCreatedEvent;
import com.edtech.platform.common.event.payment.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.edtech.platform.common.event.StudentLifecycleEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final MailService emailService;
    private final IdentityFacade identityFacade;
    private final com.edtech.platform.teacher.facade.TeacherFacade teacherFacade;

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
                teacherFacade.getTeacher(event.getTeacherId()).userId(),
                "PAYMENT_SUCCEEDED",
                title,
                content,
                "INVOICE",
                event.getInvoiceId()
        );

        // Send Email to Parent if needed
        identityFacade.getIdentity(event.getStudentId()).ifPresent(student -> {
            if (Boolean.TRUE.equals(student.notifyParent()) && student.parentEmail() != null && !student.parentEmail().isBlank()) {
                emailService.sendNotificationEmail(student.parentEmail(), title, content);
            }
        });
    }

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
        identityFacade.getIdentity(event.getStudentId()).ifPresent(student -> {
            if (student.email() != null) {
                emailService.sendNotificationEmail(student.email(), title, content);
            }
        });
    }

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
        identityFacade.getIdentity(event.getStudentId()).ifPresent(student -> {
            if (student.email() != null) {
                emailService.sendNotificationEmail(student.email(), title, content);
            }
            if (Boolean.TRUE.equals(student.notifyParent()) && student.parentEmail() != null && !student.parentEmail().isBlank()) {
                emailService.sendNotificationEmail(student.parentEmail(), title, content);
            }
        });
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudentLifecycleEvent(StudentLifecycleEvent event) {
        notificationService.createNotification(event.studentUserId(), event.type(), event.title(),
                event.content(), event.referenceType(), event.resourceId());
    }
}

