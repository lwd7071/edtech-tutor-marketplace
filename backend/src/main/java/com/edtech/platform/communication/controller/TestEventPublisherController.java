package com.edtech.platform.communication.controller;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.common.event.booking.BookingCompletedEvent;
import com.edtech.platform.common.event.booking.BookingCreatedEvent;
import com.edtech.platform.common.event.payment.PaymentSucceededEvent;
import com.edtech.platform.common.security.RequireRole;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.context.annotation.Profile;
import java.time.Instant;
import java.util.UUID;

// TEMPORARY - remove after B implements real Payment/Booking domain events
@Slf4j
@RestController
@RequestMapping("/api/internal/test-events")
@RequiredArgsConstructor
@Profile("!prod")
public class TestEventPublisherController {

    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/publish")
    @RequireRole("ADMIN")
    public void publishTestEvent(@RequestBody TestEventRequest request) {
        log.info("Publishing test event: {}", request.getEventType());
        
        switch (request.getEventType()) {
            case "PAYMENT_SUCCEEDED":
                eventPublisher.publishEvent(new PaymentSucceededEvent(
                        request.getStudentId(),
                        request.getTeacherId(),
                        UUID.randomUUID(), // invoiceId
                        UUID.randomUUID(), // pricingPackageId
                        request.getPackageName() != null ? request.getPackageName() : "Gói học thử nghiệm",
                        request.getAmountVnd() != null ? request.getAmountVnd() : 500000L
                ));
                break;
            case "BOOKING_CREATED":
                eventPublisher.publishEvent(new BookingCreatedEvent(
                        UUID.randomUUID(), // bookingId
                        request.getStudentId(),
                        request.getTeacherId(),
                        UUID.randomUUID(), // subjectId
                        request.getSubjectName() != null ? request.getSubjectName() : "Toán 12",
                        Instant.now().plusSeconds(86400),
                        Instant.now().plusSeconds(90000)
                ));
                break;
            case "BOOKING_COMPLETED":
                eventPublisher.publishEvent(new BookingCompletedEvent(
                        UUID.randomUUID(), // bookingId
                        request.getStudentId(),
                        request.getTeacherId(),
                        request.getSubjectName() != null ? request.getSubjectName() : "Toán 12"
                ));
                break;
            default:
                throw new IllegalArgumentException("Unknown event type: " + request.getEventType());
        }
    }

    @Data
    public static class TestEventRequest {
        private String eventType;
        private UUID studentId;
        private UUID teacherId;
        private String packageName;
        private Long amountVnd;
        private String subjectName;
    }
}
