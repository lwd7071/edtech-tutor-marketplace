package com.edtech.platform.communication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import com.edtech.platform.common.persistence.BaseEntity;

@Entity
@Table(name = "conversations")
@SQLDelete(sql = "UPDATE conversations SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Conversation extends BaseEntity {

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    public void markLastMessageAt(Instant time) {
        this.lastMessageAt = time;
    }
}
