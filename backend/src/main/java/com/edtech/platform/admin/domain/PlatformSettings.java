package com.edtech.platform.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "platform_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformSettings {

    @Id
    private UUID id;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "bayesian_minimum_reviews", nullable = false)
    private int bayesianMinimumReviews;

    @Column(name = "booking_reminder_hours", nullable = false)
    private int bookingReminderHours;

    @Column(name = "booking_expiration_hours", nullable = false)
    private int bookingExpirationHours;

    @Column(name = "is_singleton", nullable = false, unique = true)
    private boolean isSingleton = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PlatformSettings(BigDecimal commissionRate, int bayesianMinimumReviews,
                            int bookingReminderHours, int bookingExpirationHours) {
        this.id = UUID.randomUUID();
        this.commissionRate = Objects.requireNonNull(commissionRate);
        this.bayesianMinimumReviews = bayesianMinimumReviews;
        this.bookingReminderHours = bookingReminderHours;
        this.bookingExpirationHours = bookingExpirationHours;
        this.isSingleton = true;
        this.updatedAt = Instant.now();
    }

    public void update(BigDecimal commissionRate, int bayesianMinimumReviews,
                       int bookingReminderHours, int bookingExpirationHours) {
        if (commissionRate != null) {
            if (commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(new BigDecimal("100.00")) > 0) {
                throw new IllegalArgumentException("commission rate must be between 0 and 100");
            }
            this.commissionRate = commissionRate;
        }
        if (bayesianMinimumReviews >= 0) {
            this.bayesianMinimumReviews = bayesianMinimumReviews;
        }
        if (bookingReminderHours > 0) {
            this.bookingReminderHours = bookingReminderHours;
        }
        if (bookingExpirationHours > 0) {
            this.bookingExpirationHours = bookingExpirationHours;
        }
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}