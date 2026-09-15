package com.edtech.platform.teacher.integration;

import com.edtech.platform.catalog.dto.TeacherPublicDetail;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.common.AbstractIntegrationTest;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.teacher.domain.CredentialStatus;
import com.edtech.platform.teacher.domain.TeacherCredential;
import com.edtech.platform.teacher.repository.TeacherCredentialRepository;
import com.edtech.platform.teacher.service.TeacherCredentialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TeacherCredentialIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TeacherCredentialService credentialService;

    @Autowired
    private TeacherCredentialRepository credentialRepository;

    @Autowired
    private TeacherMarketplaceService marketplaceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private CredentialProofStorage fakeStorage;

    private static final byte[] PDF_BYTES = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes();
    private static final byte[] PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52, 0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89};

    private UUID teacherUserId;
    private UUID teacherProfileId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() throws IOException {
        teacherUserId = UUID.randomUUID();
        teacherProfileId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO users (id, email, password_hash, full_name, role, status) VALUES (?, ?, ?, ?, ?, ?)",
                teacherUserId, "teacher-" + teacherUserId + "@test.com", "hash", "Teacher Test", "TEACHER", "ACTIVE");
        jdbcTemplate.update("INSERT INTO teacher_profiles (id, user_id, bio, profile_status, is_visible) VALUES (?, ?, ?, ?, ?)",
                teacherProfileId, teacherUserId, "Teacher Bio", "APPROVED", true);

        jdbcTemplate.update("INSERT INTO users (id, email, password_hash, full_name, role, status) VALUES (?, ?, ?, ?, ?, ?)",
                adminUserId, "admin-" + adminUserId + "@test.com", "hash", "Admin Test", "ADMIN", "ACTIVE");

        when(fakeStorage.upload(any(), any())).thenAnswer(inv -> {
            String folder = inv.getArgument(1);
            String pubId = folder + "/" + UUID.randomUUID();
            return new CredentialProofStorage.StoredFile(pubId, "https://storage.test/" + pubId, "application/pdf", 1024L);
        });
    }

    @Test
    @DisplayName("Two concurrent updates on same credential: only one succeeds, the other fails with 409")
    void concurrentUpdates_onlyOneSucceeds_theOtherGets409() throws Exception {
        var file = new MockMultipartFile("proof", "degree.pdf", "application/pdf", PDF_BYTES);
        var created = credentialService.create(teacherUserId, "Initial Degree", file);
        UUID credId = created.view().id();
        long initialVersion = created.view().version();

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                try {
                    credentialService.update(teacherUserId, credId, "Updated Label " + index, null, initialVersion);
                    successCount.incrementAndGet();
                    return true;
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.CONCURRENT_MODIFICATION) {
                        conflictCount.incrementAndGet();
                    }
                    return false;
                } catch (org.springframework.dao.OptimisticLockingFailureException | jakarta.persistence.OptimisticLockException e) {
                    conflictCount.incrementAndGet();
                    return false;
                }
            }));
        }

        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        TeacherCredential after = credentialRepository.findById(credId).orElseThrow();
        assertThat(after.getVersion()).isEqualTo(initialVersion + 1);
    }

    @Test
    @DisplayName("Update, delete, approve, reject with stale version throws 409 CONCURRENT_MODIFICATION")
    void staleVersion_throws409ConcurrentModification() {
        var file = new MockMultipartFile("proof", "degree.pdf", "application/pdf", PDF_BYTES);
        var created = credentialService.create(teacherUserId, "Cert Stale", file);
        UUID credId = created.view().id();
        long initialVersion = created.view().version(); // 0

        // 1. Update with stale version (-1 or wrong)
        BusinessException exUpdate = assertThrows(BusinessException.class, () ->
                credentialService.update(teacherUserId, credId, "New", null, 999L));
        assertThat(exUpdate.getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);

        // 2. Delete with stale version
        BusinessException exDelete = assertThrows(BusinessException.class, () ->
                credentialService.delete(teacherUserId, credId, 999L));
        assertThat(exDelete.getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);

        // 3. Approve with stale version
        BusinessException exApprove = assertThrows(BusinessException.class, () ->
                credentialService.approve(credId, adminUserId, 999L));
        assertThat(exApprove.getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);

        // 4. Reject with stale version
        BusinessException exReject = assertThrows(BusinessException.class, () ->
                credentialService.reject(credId, "Bad doc", adminUserId, 999L));
        assertThat(exReject.getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);

        // Verify data didn't change
        TeacherCredential unchanged = credentialRepository.findById(credId).orElseThrow();
        assertThat(unchanged.getVersion()).isEqualTo(initialVersion);
        assertThat(unchanged.getVerificationStatus()).isEqualTo(CredentialStatus.PENDING);
    }

    @Test
    @DisplayName("Rollback cleans up uploaded proof and leaves no credential or audit")
    void rollbackCleansUpProofAndLeavesNoCredentialOrAudit() throws Exception {
        var file = new MockMultipartFile("proof", "ielts.pdf", "application/pdf", PDF_BYTES);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThrows(RuntimeException.class, () -> {
            tx.execute(status -> {
                credentialService.create(teacherUserId, "IELTS 8.0", file);
                throw new RuntimeException("Simulated database failure after upload");
            });
        });

        // 1. Storage cleanup called for the uploaded publicId
        verify(fakeStorage, atLeastOnce()).delete(any());

        // 2. Database has 0 credentials for this teacher
        long count = credentialRepository.countByTeacherIdAndDeletedFalse(teacherProfileId);
        assertThat(count).isEqualTo(0);

        // 3. No orphan audit log created
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_logs WHERE target_type = 'TEACHER_CREDENTIAL' AND actor_id = ?",
                Integer.class, teacherUserId);
        assertThat(auditCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Update success deletes old proof after transaction commit")
    void updateSuccess_deletesOldProofAfterCommit() throws Exception {
        var file1 = new MockMultipartFile("proof", "old.pdf", "application/pdf", PDF_BYTES);
        var created = credentialService.create(teacherUserId, "Cert", file1);
        UUID credId = created.view().id();
        TeacherCredential original = credentialRepository.findById(credId).orElseThrow();
        String oldPublicId = original.getCloudinaryPublicId();

        var file2 = new MockMultipartFile("proof", "new.png", "image/png", PNG_BYTES);
        credentialService.update(teacherUserId, credId, "Cert Updated", file2, original.getVersion());

        // After commit, fakeStorage.delete(oldPublicId) must be executed
        verify(fakeStorage).delete(eq(oldPublicId));
    }

    @Test
    @DisplayName("Public detail only contains approved credential labels and ids, no URLs or bytes")
    void publicDetail_onlyContainsApprovedBadges_noUrlsOrBytes() throws Exception {
        // 1. Create a credential that stays PENDING
        var file1 = new MockMultipartFile("proof", "pending.pdf", "application/pdf", PDF_BYTES);
        var pendingCred = credentialService.create(teacherUserId, "Pending Degree", file1);

        // 2. Create another credential and approve it
        var file2 = new MockMultipartFile("proof", "approved.pdf", "application/pdf", PDF_BYTES);
        var toApprove = credentialService.create(teacherUserId, "Approved IELTS 8.5", file2);
        credentialService.approve(toApprove.view().id(), adminUserId, 0L);

        // 3. Create another credential and reject it
        var file3 = new MockMultipartFile("proof", "rejected.pdf", "application/pdf", PDF_BYTES);
        var toReject = credentialService.create(teacherUserId, "Rejected Cert", file3);
        credentialService.reject(toReject.view().id(), "Blurry document", adminUserId, 0L);

        // 4. Fetch public detail
        TeacherPublicDetail detail = marketplaceService.getTeacherDetail(teacherProfileId);

        assertThat(detail.credentials()).hasSize(1);
        var badge = detail.credentials().get(0);
        assertThat(badge.id()).isEqualTo(toApprove.view().id());
        assertThat(badge.label()).isEqualTo("Approved IELTS 8.5");

        // Assert no URL or bytes exposed
        assertThat(badge.toString()).doesNotContain("http");
        assertThat(badge.toString()).doesNotContain("cloudinary");
        assertThat(badge.toString()).doesNotContain("storage");
    }
}
