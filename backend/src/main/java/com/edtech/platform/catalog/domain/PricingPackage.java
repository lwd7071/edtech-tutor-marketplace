package com.edtech.platform.catalog.domain;

import com.edtech.platform.common.persistence.BaseEntity;
import com.edtech.platform.subject.domain.Subject;
import com.edtech.platform.teacher.domain.TeacherProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "pricing_packages")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE pricing_packages SET is_deleted = true WHERE id = ? and version = ?")
@Where(clause = "is_deleted = false")
public class PricingPackage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private TeacherProfile teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "total_sessions", nullable = false)
    private int totalSessions;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "price_vnd", nullable = false)
    private long priceVnd;

    @Column(name = "session_duration_minutes", nullable = false)
    private int sessionDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PackageStatus status = PackageStatus.ACTIVE;

    @Version
    @Column(nullable = false)
    private long version;

    @Builder
    public PricingPackage(TeacherProfile teacher, Subject subject, String name, String description, int totalSessions, int durationDays, long priceVnd, int sessionDurationMinutes, PackageStatus status) {
        this.teacher = teacher;
        this.subject = subject;
        this.name = name;
        this.description = description;
        this.totalSessions = totalSessions;
        this.durationDays = durationDays;
        this.priceVnd = priceVnd;
        this.sessionDurationMinutes = sessionDurationMinutes;
        if (status != null) {
            this.status = status;
        }
    }
}
