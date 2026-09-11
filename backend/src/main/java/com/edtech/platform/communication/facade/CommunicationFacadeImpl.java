package com.edtech.platform.communication.facade;

import com.edtech.platform.booking.facade.BookingEvent;
import com.edtech.platform.booking.facade.CommunicationFacade;
import com.edtech.platform.communication.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunicationFacadeImpl implements CommunicationFacade {

    private final NotificationService notifications;
    private final ApplicationEventPublisher publisher;
    private final com.edtech.platform.teacher.facade.TeacherFacade teacherFacade;
    private final com.edtech.platform.communication.service.ConversationService conversations;

    @Override
    public void publishAfterCommit(BookingEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void sendBookingReminder(UUID userId, UUID bookingId) {
        notifications.createNotification(userId, "BOOKING_REMINDER", "Booking reminder", "Upcoming booking reminder", "BOOKING", bookingId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingEvent(BookingEvent e) {
        if ("TRIAL_REQUESTED".equals(e.type())) {
            notifications.createNotification(e.teacherUserId(), e.type(), "Yêu cầu học thử mới", "Một học viên vừa gửi yêu cầu học thử", "BOOKING", e.resourceId());
            return;
        }
        String title = switch (e.type()) {
            case "TRIAL_ACCEPTED" -> "Yêu cầu học thử đã được chấp nhận";
            case "TRIAL_REJECTED" -> "Yêu cầu học thử đã bị từ chối";
            case "BOOKING_CREATED" -> "Lịch học mới";
            case "BOOKING_COMPLETED" -> "Buổi học đã hoàn thành";
            case "BOOKING_CANCELLED" -> "Buổi học đã hủy";
            default -> "Cập nhật lịch học";
        };
        String referenceType = "TRIAL_REJECTED".equals(e.type()) ? "TRIAL_REQUEST" : "BOOKING";
        notifications.createNotification(e.studentUserId(), e.type(), title, title, referenceType, e.resourceId());
        if ("TRIAL_ACCEPTED".equals(e.type()) || "BOOKING_CREATED".equals(e.type())) {
            conversations.getOrCreateConversation(e.teacherProfileId(), e.studentUserId());
        }
    }
}
