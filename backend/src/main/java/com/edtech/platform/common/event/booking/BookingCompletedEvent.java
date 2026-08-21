package com.edtech.platform.common.event.booking;

import com.edtech.platform.common.event.AbstractDomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class BookingCompletedEvent extends AbstractDomainEvent {
    private final UUID bookingId;
    private final UUID studentId;
    private final UUID teacherId;
    private final String subjectName;

    public BookingCompletedEvent(UUID bookingId, UUID studentId, UUID teacherId, String subjectName) {
        super();
        this.bookingId = bookingId;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.subjectName = subjectName;
    }
}
