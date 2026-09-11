package com.edtech.platform.finance.domain;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "package_extension_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE package_extension_requests SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PackageExtensionRequest extends BaseEntity {

    @Column(name = "student_package_id", nullable = false)
    private UUID studentPackageId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "requested_expiry_date", nullable = false)
    private Instant requestedExpiryDate;

    @Column(name = "approved_expiry_date")
    private Instant approvedExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExtensionStatus status;

    @Column(name = "admin_note", columnDefinition = "text")
    private String adminNote;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public static PackageExtensionRequest create(
            UUID studentPackageId,
            UUID studentId,
            String reason,
            Instant requestedExpiryDate,
            Instant now
    ) {
        if (requestedExpiryDate == null || now == null || !requestedExpiryDate.isAfter(now)) {
            throw new BusinessException(ErrorCode.PACKAGE_EXTENSION_DATE_INVALID);
        }
        PackageExtensionRequest req = new PackageExtensionRequest();
        req.studentPackageId = Objects.requireNonNull(studentPackageId);
        req.studentId = Objects.requireNonNull(studentId);
        req.reason = reason;
        req.requestedExpiryDate = requestedExpiryDate;
        req.status = ExtensionStatus.PENDING;
        return req;
    }

    public void approve(UUID adminId, Instant approvedExpiryDate, String adminNote, Instant now, Instant at) {
        if (status != ExtensionStatus.PENDING) {
            throw new BusinessException(ErrorCode.EXTENSION_INVALID_STATE);
        }
        if (approvedExpiryDate == null || now == null || !approvedExpiryDate.isAfter(now)) {
            throw new BusinessException(ErrorCode.PACKAGE_EXTENSION_DATE_INVALID);
        }
        this.status = ExtensionStatus.APPROVED;
        this.approvedExpiryDate = approvedExpiryDate;
        this.adminNote = adminNote;
        this.reviewedBy = Objects.requireNonNull(adminId);
        this.reviewedAt = Objects.requireNonNull(at);
    }

    public void reject(UUID adminId, String adminNote, Instant at) {
        if (status != ExtensionStatus.PENDING) {
            throw new BusinessException(ErrorCode.EXTENSION_INVALID_STATE);
        }
        this.status = ExtensionStatus.REJECTED;
        this.adminNote = adminNote;
        this.reviewedBy = Objects.requireNonNull(adminId);
        this.reviewedAt = Objects.requireNonNull(at);
    }
}
