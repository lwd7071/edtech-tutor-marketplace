package com.edtech.platform.teacher.domain;

import com.edtech.platform.common.persistence.BaseEntity;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teacher_profiles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE teacher_profiles SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class TeacherProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "languages", columnDefinition = "text[]")
    private List<String> languages = new ArrayList<>();

    @Column(name = "supports_online", nullable = false)
    private boolean supportsOnline = true;

    @Column(name = "supports_offline", nullable = false)
    private boolean supportsOffline = false;

    @Column(name = "location_address", columnDefinition = "text")
    private String locationAddress;

    @Column(name = "introduction_video_url", length = 500)
    private String introductionVideoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status", nullable = false, length = 20)
    private ProfileStatus profileStatus = ProfileStatus.DRAFT;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "verified_badge", nullable = false)
    private boolean verifiedBadge = false;

    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = false;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private UUID approvedById;

    @Builder
    public TeacherProfile(UUID userId) {
        this.userId = userId;
        this.profileStatus = ProfileStatus.DRAFT;
        this.languages = new ArrayList<>();
    }

    public void submitForApproval() {
        if (this.profileStatus == ProfileStatus.DRAFT || this.profileStatus == ProfileStatus.REJECTED) {
            this.profileStatus = ProfileStatus.PENDING_APPROVAL;
        } else {
            throw new IllegalStateException("Cannot submit profile in state " + this.profileStatus);
        }
    }

    public void approve(UUID adminId) {
        if (this.profileStatus != ProfileStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot approve profile in state " + this.profileStatus);
        }
        this.profileStatus = ProfileStatus.APPROVED;
        this.approvedById = adminId;
        this.approvedAt = Instant.now();
        this.rejectionReason = null;
        this.isVisible = true;
    }

    public void reject(String reason) {
        if (this.profileStatus != ProfileStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot reject profile in state " + this.profileStatus);
        }
        this.profileStatus = ProfileStatus.REJECTED;
        this.rejectionReason = reason;
        this.isVisible = false;
    }
}
