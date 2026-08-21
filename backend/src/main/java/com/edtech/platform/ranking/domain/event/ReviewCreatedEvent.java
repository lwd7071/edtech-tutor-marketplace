package com.edtech.platform.ranking.domain.event;

import com.edtech.platform.common.event.AbstractDomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ReviewCreatedEvent extends AbstractDomainEvent {
    private final UUID reviewId;
    private final UUID teacherId;
    private final UUID studentId;
    private final Integer rating;

    public ReviewCreatedEvent(UUID reviewId, UUID teacherId, UUID studentId, Integer rating) {
        super();
        this.reviewId = reviewId;
        this.teacherId = teacherId;
        this.studentId = studentId;
        this.rating = rating;
    }
}
