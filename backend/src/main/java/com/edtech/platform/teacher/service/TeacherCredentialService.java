package com.edtech.platform.teacher.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.domain.*;
import com.edtech.platform.teacher.dto.*;
import com.edtech.platform.teacher.repository.*;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.admin.facade.AuditTrailFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service @RequiredArgsConstructor @Slf4j
public class TeacherCredentialService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_CREDENTIALS = 10;
    private static final List<String> ALLOWED = List.of("image/jpeg", "image/png", "application/pdf");
    private final TeacherCredentialRepository repository;
    private final TeacherProfileRepository profiles;
    private final CredentialProofStorage storage;
    private final AuditTrailFacade auditTrail;
    private final com.edtech.platform.common.cache.PublicCacheRevalidationClient revalidationClient;
    private final Tika tika = new Tika();

    @Transactional(readOnly = true)
    public List<TeacherCredentialView> list(UUID userId) {
        TeacherProfile p = profile(userId);
        return repository.findByTeacherIdOrderByCreatedAtDesc(p.getId()).stream().map(this::view).toList();
    }

    @Transactional
    @Caching(evict = {@CacheEvict(value = "TEACHER_PUBLIC_PROFILE", key = "#result.teacherId"), @CacheEvict(value = "POPULAR_SEARCH", allEntries = true)})
    public CredentialResult create(UUID userId, String label, MultipartFile proof) {
        TeacherProfile p = profile(userId);
        if (repository.countByTeacherIdAndDeletedFalse(p.getId()) >= MAX_CREDENTIALS) throw new BusinessException(ErrorCode.CREDENTIAL_LIMIT_REACHED);
        ValidFile f = validate(proof);
        try {
            var stored = storage.upload(proof, "teacher_credentials/" + p.getId());
            registerCleanupOnRollback(stored.publicId());
            TeacherCredential c = repository.save(TeacherCredential.builder().teacher(p).label(normalizeLabel(label))
                    .evidencePublicId(stored.publicId()).evidenceResourceType("auto")
                    .evidenceFormat(TeacherCredential.formatOf(f.mime()))
                    .evidenceMimeType(f.mime()).evidenceSize(f.size()).build());
            recordAudit(userId, "TEACHER_CREDENTIAL_CREATED", c, null, c.getLabel());
            return new CredentialResult(p.getId(), view(c));
        } catch (IOException e) { throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED); }
    }

    @Transactional
    @Caching(evict = {@CacheEvict(value = "TEACHER_PUBLIC_PROFILE", key = "#result.teacherId"), @CacheEvict(value = "POPULAR_SEARCH", allEntries = true)})
    public CredentialResult update(UUID userId, UUID id, String label, MultipartFile proof) { return update(userId, id, label, proof, null); }

    @Transactional
    @Caching(evict = {@CacheEvict(value = "TEACHER_PUBLIC_PROFILE", key = "#result.teacherId"), @CacheEvict(value = "POPULAR_SEARCH", allEntries = true)})
    public CredentialResult update(UUID userId, UUID id, String label, MultipartFile proof, Long expectedVersion) {
        TeacherProfile p = profile(userId); TeacherCredential c = owned(id, p.getId());
        requireVersion(c, expectedVersion);
        boolean wasApproved = c.getVerificationStatus() == CredentialStatus.APPROVED;
        String beforeLabel = c.getLabel(); String old = c.getCloudinaryPublicId();
        try {
            String normalized = normalizeLabel(label);
            if (proof == null || proof.isEmpty()) {
                c.rename(normalized);
                recordAudit(userId, "TEACHER_CREDENTIAL_UPDATED", c, beforeLabel, normalized);
                if (wasApproved && revalidationClient != null) {
                    revalidationClient.revalidateTeacherPublicData(p.getId(), "TEACHER_CREDENTIAL_REVOKED");
                }
                return new CredentialResult(p.getId(), view(c));
            }
            ValidFile f = validate(proof); var stored = storage.upload(proof, "teacher_credentials/" + p.getId());
            registerCleanupOnRollback(stored.publicId());
            c.edit(label.trim(), stored.publicId(), stored.secureUrl(), f.mime(), f.size());
            registerCleanupAfterCommit(old);
            recordAudit(userId, "TEACHER_CREDENTIAL_UPDATED", c, beforeLabel, normalized);
            if (wasApproved && revalidationClient != null) {
                revalidationClient.revalidateTeacherPublicData(p.getId(), "TEACHER_CREDENTIAL_REVOKED");
            }
            return new CredentialResult(p.getId(), view(c));
        } catch (IOException e) { throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED); }
    }

    @Transactional
    @Caching(evict = {@CacheEvict(value = "TEACHER_PUBLIC_PROFILE", key = "#result"), @CacheEvict(value = "POPULAR_SEARCH", allEntries = true)})
    public UUID delete(UUID userId, UUID id) { return delete(userId, id, null); }
    @Transactional
    @Caching(evict = {@CacheEvict(value = "TEACHER_PUBLIC_PROFILE", key = "#result"), @CacheEvict(value = "POPULAR_SEARCH", allEntries = true)})
    public UUID delete(UUID userId, UUID id, Long expectedVersion) {
        TeacherProfile p = profile(userId); TeacherCredential c = owned(id, p.getId());
        requireVersion(c, expectedVersion);
        boolean wasApproved = c.getVerificationStatus() == CredentialStatus.APPROVED;
        c.setDeleted(true); registerCleanupAfterCommit(c.getCloudinaryPublicId());
        recordAudit(userId, "TEACHER_CREDENTIAL_DELETED", c, c.getLabel(), null);
        if (wasApproved && revalidationClient != null) {
            revalidationClient.revalidateTeacherPublicData(p.getId(), "TEACHER_CREDENTIAL_REVOKED");
        }
        return p.getId();
    }

    @Transactional
    @Caching(evict = {@CacheEvict(value = "TEACHER_PUBLIC_PROFILE", key = "#result.teacherId"), @CacheEvict(value = "POPULAR_SEARCH", allEntries = true)})
    public CredentialResult approve(UUID id, UUID adminId) { return approve(id, adminId, null); }
    @Transactional
    @Caching(evict = {@CacheEvict(value = "TEACHER_PUBLIC_PROFILE", key = "#result.teacherId"), @CacheEvict(value = "POPULAR_SEARCH", allEntries = true)})
    public CredentialResult approve(UUID id, UUID adminId, Long expectedVersion) {
        TeacherCredential c = credentialForUpdate(id); requireVersion(c, expectedVersion); c.approve(adminId);
        recordAudit(adminId, "TEACHER_CREDENTIAL_APPROVED", c, CredentialStatus.PENDING, CredentialStatus.APPROVED);
        if (revalidationClient != null) {
            revalidationClient.revalidateTeacherPublicData(c.getTeacher().getId(), "TEACHER_CREDENTIAL_APPROVED");
        }
        return new CredentialResult(c.getTeacher().getId(), view(c));
    }

    @Transactional
    @Caching(evict = {@CacheEvict(value = "TEACHER_PUBLIC_PROFILE", key = "#result.teacherId"), @CacheEvict(value = "POPULAR_SEARCH", allEntries = true)})
    public CredentialResult reject(UUID id, String reason) { return reject(id, reason, null, null); }
    @Transactional
    @Caching(evict = {@CacheEvict(value = "TEACHER_PUBLIC_PROFILE", key = "#result.teacherId"), @CacheEvict(value = "POPULAR_SEARCH", allEntries = true)})
    public CredentialResult reject(UUID id, String reason, UUID adminId, Long expectedVersion) { TeacherCredential c = credentialForUpdate(id); requireVersion(c, expectedVersion); if (reason == null || reason.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR); c.reject(reason.trim()); recordAudit(adminId, "TEACHER_CREDENTIAL_REJECTED", c, CredentialStatus.PENDING, CredentialStatus.REJECTED); return new CredentialResult(c.getTeacher().getId(), view(c)); }

    @Transactional(readOnly = true)
    public List<PublicCredentialBadge> approvedBadges(UUID teacherId) {
        return repository.findByTeacherIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(teacherId, CredentialStatus.APPROVED)
                .stream().map(c -> new PublicCredentialBadge(c.getId(), c.getLabel())).toList();
    }
    @Transactional(readOnly = true)
    public String proofUrl(UUID userId, UUID id) {
        TeacherProfile p = profile(userId); TeacherCredential c = owned(id, p.getId());
        return storage.signedUrl(c.getCloudinaryPublicId());
    }
    @Transactional(readOnly = true)
    public CredentialProofStorage.DownloadedFile proof(UUID userId, UUID id) throws IOException {
        TeacherProfile p = profile(userId); TeacherCredential c = owned(id, p.getId());
        return storage.download(c.getCloudinaryPublicId());
    }
    @Transactional(readOnly = true)
    public List<AdminCredentialView> adminList(String status) {
        try {
            return adminList(CredentialStatus.valueOf(status.toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public List<AdminCredentialView> adminList(CredentialStatus status) {
        return repository.findByStatusAndDeletedFalseOrderByCreatedAtAsc(status).stream()
                .map(c -> new AdminCredentialView(c.getId(), c.getTeacher().getId(), c.getLabel(), "/api/admin/credentials/" + c.getId() + "/proof", c.getVerificationStatus(), c.getRejectedReason(), c.getCreatedAt(), c.getVersion())).toList();
    }
    @Transactional(readOnly = true)
    public String adminProofUrl(UUID id) { return storage.signedUrl(credential(id).getCloudinaryPublicId()); }
    @Transactional(readOnly = true)
    public CredentialProofStorage.DownloadedFile adminProof(UUID id) throws IOException {
        return storage.download(credential(id).getCloudinaryPublicId());
    }
    private TeacherProfile profile(UUID userId) { return profiles.findByUserId(userId).orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND)); }
    private TeacherCredential credential(UUID id) { return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CREDENTIAL_NOT_FOUND)); }
    private TeacherCredential owned(UUID id, UUID teacherId) { TeacherCredential c = credential(id); if (!c.getTeacher().getId().equals(teacherId)) throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE); return c; }
    private ValidFile validate(MultipartFile f) {
        if (f == null || f.isEmpty()) throw new BusinessException(ErrorCode.CREDENTIAL_PROOF_REQUIRED);
        if (f.getSize() > MAX_FILE_SIZE) throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        try { String mime = tika.detect(f.getInputStream()); if (!ALLOWED.contains(mime) || !extensionMatches(f.getOriginalFilename(), mime)) throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED); return new ValidFile(mime, f.getSize()); }
        catch (IOException e) { throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED); }
    }
    private TeacherCredentialView view(TeacherCredential c) { return new TeacherCredentialView(c.getId(), c.getLabel(), c.getVerificationStatus(), "/api/teacher/credentials/" + c.getId() + "/proof", c.getRejectedReason(), c.getVerifiedAt(), c.getVersion()); }
    private String normalizeLabel(String label) { if (label == null || label.isBlank() || label.trim().length() > 120) throw new BusinessException(ErrorCode.VALIDATION_ERROR); return label.trim(); }
    private boolean extensionMatches(String name, String mime) { if (name == null || !name.contains(".")) return false; String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT); return ("jpg".equals(ext) || "jpeg".equals(ext)) && "image/jpeg".equals(mime) || "png".equals(ext) && "image/png".equals(mime) || "pdf".equals(ext) && "application/pdf".equals(mime); }
    private void requireVersion(TeacherCredential c, Long expected) { if (expected != null && (expected < 0 || expected != c.getVersion())) throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION); }
    private TeacherCredential credentialForUpdate(UUID id) { return repository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.CREDENTIAL_NOT_FOUND)); }
    private void recordAudit(UUID actor, String action, TeacherCredential c, Object before, Object after) { if (auditTrail != null) auditTrail.append(actor, action, "TEACHER_CREDENTIAL", c.getId(), java.util.Map.of("value", before == null ? "" : before), java.util.Map.of("value", after == null ? "" : after)); }
    private void registerCleanupOnRollback(String publicId) { if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){ public void afterCompletion(int status){ if(status != STATUS_COMMITTED) try { storage.delete(publicId); } catch(IOException e){ log.error("credential cleanup failed after rollback", e); } }}); }
    private void registerCleanupAfterCommit(String publicId) { if (publicId == null || publicId.isBlank() || !TransactionSynchronizationManager.isSynchronizationActive()) return; TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){ public void afterCommit(){ try { storage.delete(publicId); } catch(IOException e){ log.error("credential cleanup failed after commit", e); } }}); }
    private record ValidFile(String mime, long size) {}
    public record CredentialResult(UUID teacherId, TeacherCredentialView view) {}
}
