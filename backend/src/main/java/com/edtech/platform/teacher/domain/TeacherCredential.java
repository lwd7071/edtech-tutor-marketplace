package com.edtech.platform.teacher.domain;

import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teacher_credentials")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE teacher_credentials SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class TeacherCredential extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private TeacherProfile teacher;
    @Column(nullable = false, length = 120) private String label;
    @Column(name = "evidence_public_id", nullable = false, length = 255) private String evidencePublicId;
    @Column(name = "evidence_resource_type", nullable = false, length = 20) private String evidenceResourceType;
    @Column(name = "evidence_format", nullable = false, length = 20) private String evidenceFormat;
    @Column(name = "evidence_mime_type", nullable = false, length = 100) private String evidenceMimeType;
    @Column(name = "evidence_size", nullable = false) private Long evidenceSize;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20)
    private CredentialStatus status = CredentialStatus.PENDING;
    @Column(name = "rejected_reason") private String rejectedReason;
    @Column(name = "approved_by") private UUID approvedBy;
    @Column(name = "approved_at") private Instant approvedAt;
    @Version @Column(name = "version", nullable = false) private long version;

    @Builder
    public TeacherCredential(TeacherProfile teacher, String label, String evidencePublicId,
                             String evidenceResourceType, String evidenceFormat,
                             String evidenceMimeType, Long evidenceSize) {
        this.teacher = teacher; this.label = label; this.evidencePublicId = evidencePublicId;
        this.evidenceResourceType = evidenceResourceType; this.evidenceFormat = evidenceFormat;
        this.evidenceMimeType = evidenceMimeType; this.evidenceSize = evidenceSize;
    }
    public void edit(String label, String publicId, String url, String mimeType, long size) {
        this.label = label; this.evidencePublicId = publicId; this.evidenceMimeType = mimeType;
        this.evidenceFormat = formatOf(mimeType); this.evidenceSize = size; this.status = CredentialStatus.PENDING;
        this.rejectedReason = null; this.approvedBy = null; this.approvedAt = null;
    }
    public void rename(String label) { this.label = label; this.status = CredentialStatus.PENDING; this.rejectedReason = null; this.approvedBy = null; this.approvedAt = null; }
    public void approve(UUID adminId) { this.status = CredentialStatus.APPROVED; this.approvedBy = adminId; this.approvedAt = Instant.now(); this.rejectedReason = null; }
    public void reject(String reason) { this.status = CredentialStatus.REJECTED; this.rejectedReason = reason; this.approvedBy = null; this.approvedAt = null; }
    public CredentialStatus getVerificationStatus() { return status; }
    public String getCloudinaryPublicId() { return evidencePublicId; }
    public Instant getVerifiedAt() { return approvedAt; }
    public long getVersion() { return version; }
    public static String formatOf(String mime) {
        if ("image/jpeg".equals(mime)) return "jpg";
        if ("image/png".equals(mime)) return "png";
        if ("application/pdf".equals(mime)) return "pdf";
        throw new IllegalArgumentException("Unsupported mime type: " + mime);
    }
}
