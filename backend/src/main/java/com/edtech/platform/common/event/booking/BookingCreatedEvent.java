package com.edtech.platform.common.event.booking;

import com.edtech.platform.common.event.AbstractDomainEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class BookingCreatedEvent extends AbstractDomainEvent {
    private final UUID bookingId;
    private final UUID studentId;
    private final UUID teacherId;
    private final UUID subjectId;
    private final String subjectName;
    private final Instant startTime;
    private final Instant endTime;

    public BookingCreatedEvent(UUID bookingId, UUID studentId, UUID teacherId, UUID subjectId, String subjectName, Instant startTime, Instant endTime) {
        super();
        this.bookingId = bookingId;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
