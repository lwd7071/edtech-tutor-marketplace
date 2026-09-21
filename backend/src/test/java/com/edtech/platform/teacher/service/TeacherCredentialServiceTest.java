package com.edtech.platform.teacher.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.teacher.domain.CredentialStatus;
import com.edtech.platform.teacher.domain.TeacherCredential;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.repository.TeacherCredentialRepository;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import com.edtech.platform.common.storage.CredentialProofStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherCredentialServiceTest {
    @Mock TeacherCredentialRepository repository;
    @Mock TeacherProfileRepository profiles;
    @Mock CredentialProofStorage storage;
    @Mock com.edtech.platform.admin.facade.AuditTrailFacade auditTrail;
    @Mock com.edtech.platform.common.cache.PublicCacheRevalidationClient revalidationClient;
    @InjectMocks TeacherCredentialService service;

    private static final byte[] PDF_BYTES = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes();
    private static final byte[] PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52, 0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89};
    private static final byte[] JPG_BYTES = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 0x4A, 0x46, 0x49, 0x46, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0};

    @Test
    void createStoresProofAndStartsPending_pdfFormat() throws Exception {
        UUID user = UUID.randomUUID(), teacherId = UUID.randomUUID();
        TeacherProfile profile = mock(TeacherProfile.class);
        when(profile.getId()).thenReturn(teacherId);
        when(profiles.findByUserId(user)).thenReturn(Optional.of(profile));
        when(repository.countByTeacherIdAndDeletedFalse(teacherId)).thenReturn(0L);

        var file = new MockMultipartFile("proof", "ielts.pdf", "application/pdf", PDF_BYTES);
        when(storage.upload(any(), eq("teacher_credentials/" + teacherId)))
                .thenReturn(new CredentialProofStorage.StoredFile("p1", "url1", "application/pdf", PDF_BYTES.length));

        org.mockito.ArgumentCaptor<TeacherCredential> captor = org.mockito.ArgumentCaptor.forClass(TeacherCredential.class);
        when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(user, "IELTS 7.5", file);
        assertEquals(CredentialStatus.PENDING, result.view().status());
        TeacherCredential saved = captor.getValue();
        assertEquals("pdf", saved.getEvidenceFormat());
        assertEquals("application/pdf", saved.getEvidenceMimeType());
        verify(storage).upload(any(), eq("teacher_credentials/" + teacherId));
        verify(auditTrail).append(eq(user), eq("TEACHER_CREDENTIAL_CREATED"), eq("TEACHER_CREDENTIAL"), any(), any(), any());
        verifyNoInteractions(revalidationClient);
    }

    @Test
    void createStoresProof_jpegAndPngFormats() throws Exception {
        UUID user = UUID.randomUUID(), teacherId = UUID.randomUUID();
        TeacherProfile profile = mock(TeacherProfile.class);
        when(profile.getId()).thenReturn(teacherId);
        when(profiles.findByUserId(user)).thenReturn(Optional.of(profile));
        when(repository.countByTeacherIdAndDeletedFalse(teacherId)).thenReturn(0L);

        // JPEG test
        var jpgFile = new MockMultipartFile("proof", "certificate.jpg", "image/jpeg", JPG_BYTES);
        when(storage.upload(any(), eq("teacher_credentials/" + teacherId)))
                .thenReturn(new CredentialProofStorage.StoredFile("p_jpg", "url_jpg", "image/jpeg", JPG_BYTES.length));
        org.mockito.ArgumentCaptor<TeacherCredential> captorJpg = org.mockito.ArgumentCaptor.forClass(TeacherCredential.class);
        when(repository.save(captorJpg.capture())).thenAnswer(inv -> inv.getArgument(0));
        service.create(user, "Cert JPG", jpgFile);
        assertEquals("jpg", captorJpg.getValue().getEvidenceFormat());
        assertEquals("image/jpeg", captorJpg.getValue().getEvidenceMimeType());

        // PNG test
        var pngFile = new MockMultipartFile("proof", "badge.png", "image/png", PNG_BYTES);
        when(storage.upload(any(), eq("teacher_credentials/" + teacherId)))
                .thenReturn(new CredentialProofStorage.StoredFile("p_png", "url_png", "image/png", PNG_BYTES.length));
        org.mockito.ArgumentCaptor<TeacherCredential> captorPng = org.mockito.ArgumentCaptor.forClass(TeacherCredential.class);
        when(repository.save(captorPng.capture())).thenAnswer(inv -> inv.getArgument(0));
        service.create(user, "Cert PNG", pngFile);
        assertEquals("png", captorPng.getValue().getEvidenceFormat());
        assertEquals("image/png", captorPng.getValue().getEvidenceMimeType());
    }

    @Test
    void createRejectsFakeExtension_contentMismatch() {
        UUID user = UUID.randomUUID(), teacherId = UUID.randomUUID();
        TeacherProfile p = mock(TeacherProfile.class);
        when(p.getId()).thenReturn(teacherId);
        when(profiles.findByUserId(user)).thenReturn(Optional.of(p));
        when(repository.countByTeacherIdAndDeletedFalse(teacherId)).thenReturn(0L);

        // Name is .pdf but content is JPEG bytes
        var fakePdf = new MockMultipartFile("proof", "fake.pdf", "application/pdf", JPG_BYTES);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(user, "Fake", fakePdf));
        assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED, ex.getErrorCode());
        verifyNoInteractions(storage);
    }

    @Test
    void createRejectsFakeExtension_textBytesWithJpgName() {
        UUID user = UUID.randomUUID(), teacherId = UUID.randomUUID();
        TeacherProfile p = mock(TeacherProfile.class);
        when(p.getId()).thenReturn(teacherId);
        when(profiles.findByUserId(user)).thenReturn(Optional.of(p));
        when(repository.countByTeacherIdAndDeletedFalse(teacherId)).thenReturn(0L);

        var fakeJpg = new MockMultipartFile("proof", "text.jpg", "image/jpeg", "plain text not image".getBytes());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(user, "Fake", fakeJpg));
        assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED, ex.getErrorCode());
        verifyNoInteractions(storage);
    }

    @Test
    void createRejectsOversizedFile() {
        UUID user = UUID.randomUUID(), teacherId = UUID.randomUUID();
        TeacherProfile p = mock(TeacherProfile.class);
        when(p.getId()).thenReturn(teacherId);
        when(profiles.findByUserId(user)).thenReturn(Optional.of(p));
        when(repository.countByTeacherIdAndDeletedFalse(teacherId)).thenReturn(0L);

        byte[] largeBytes = new byte[5 * 1024 * 1024 + 1];
        var largeFile = new MockMultipartFile("proof", "large.pdf", "application/pdf", largeBytes);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(user, "Big", largeFile));
        assertEquals(ErrorCode.FILE_TOO_LARGE, ex.getErrorCode());
        verifyNoInteractions(storage);
    }

    @Test
    void createRejectsAfterTenCredentials() {
        UUID user = UUID.randomUUID(), teacherId = UUID.randomUUID();
        TeacherProfile p = mock(TeacherProfile.class);
        when(p.getId()).thenReturn(teacherId);
        when(profiles.findByUserId(user)).thenReturn(Optional.of(p));
        when(repository.countByTeacherIdAndDeletedFalse(teacherId)).thenReturn(10L);
        var file = new MockMultipartFile("proof", "a.pdf", "application/pdf", PDF_BYTES);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(user, "A", file));
        assertEquals(ErrorCode.CREDENTIAL_LIMIT_REACHED, ex.getErrorCode());
        verifyNoInteractions(storage);
    }

    @Test
    void createRequiresProof() {
        UUID user = UUID.randomUUID();
        when(profiles.findByUserId(user)).thenReturn(Optional.of(mock(TeacherProfile.class)));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(user, "IELTS", null));
        assertEquals(ErrorCode.CREDENTIAL_PROOF_REQUIRED, ex.getErrorCode());
    }

    @Test
    void update_withNewProof_updatesFormatAndMime() throws Exception {
        UUID user = UUID.randomUUID(), teacherId = UUID.randomUUID(), credId = UUID.randomUUID();
        TeacherProfile profile = mock(TeacherProfile.class);
        when(profile.getId()).thenReturn(teacherId);
        when(profiles.findByUserId(user)).thenReturn(Optional.of(profile));

        TeacherCredential credential = TeacherCredential.builder()
                .teacher(profile).label("Old").evidencePublicId("old_pub").evidenceResourceType("auto")
                .evidenceFormat("pdf").evidenceMimeType("application/pdf").evidenceSize(100L).build();
        when(repository.findById(credId)).thenReturn(Optional.of(credential));
        when(storage.upload(any(), eq("teacher_credentials/" + teacherId)))
                .thenReturn(new CredentialProofStorage.StoredFile("new_pub", "new_url", "image/png", PNG_BYTES.length));

        var pngFile = new MockMultipartFile("proof", "new.png", "image/png", PNG_BYTES);
        var res = service.update(user, credId, "New Label", pngFile, 0L);

        assertEquals("New Label", credential.getLabel());
        assertEquals("png", credential.getEvidenceFormat());
        assertEquals("image/png", credential.getEvidenceMimeType());
        verify(auditTrail).append(eq(user), eq("TEACHER_CREDENTIAL_UPDATED"), eq("TEACHER_CREDENTIAL"), eq(credential.getId()), any(), any());
    }

    @Test
    void update_concurrentModification_throws409() {
        UUID user = UUID.randomUUID(), teacherId = UUID.randomUUID(), credId = UUID.randomUUID();
        TeacherProfile profile = mock(TeacherProfile.class);
        when(profile.getId()).thenReturn(teacherId);
        when(profiles.findByUserId(user)).thenReturn(Optional.of(profile));

        TeacherCredential credential = mock(TeacherCredential.class);
        when(credential.getTeacher()).thenReturn(profile);
        when(credential.getVersion()).thenReturn(2L);
        when(repository.findById(credId)).thenReturn(Optional.of(credential));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(user, credId, "Label", null, 1L));
        assertEquals(ErrorCode.CONCURRENT_MODIFICATION, ex.getErrorCode());
    }

    @Test
    void delete_concurrentModification_throws409() {
        UUID user = UUID.randomUUID(), teacherId = UUID.randomUUID(), credId = UUID.randomUUID();
        TeacherProfile profile = mock(TeacherProfile.class);
        when(profile.getId()).thenReturn(teacherId);
        when(profiles.findByUserId(user)).thenReturn(Optional.of(profile));

        TeacherCredential credential = mock(TeacherCredential.class);
        when(credential.getTeacher()).thenReturn(profile);
        when(credential.getVersion()).thenReturn(3L);
        when(repository.findById(credId)).thenReturn(Optional.of(credential));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(user, credId, 2L));
        assertEquals(ErrorCode.CONCURRENT_MODIFICATION, ex.getErrorCode());
    }

    @Test
    void approve_revalidatesAndAudits() {
        UUID credId = UUID.randomUUID(), adminId = UUID.randomUUID(), teacherId = UUID.randomUUID();
        TeacherProfile profile = mock(TeacherProfile.class);
        when(profile.getId()).thenReturn(teacherId);

        TeacherCredential credential = TeacherCredential.builder()
                .teacher(profile).label("Degree").evidencePublicId("p").evidenceResourceType("auto")
                .evidenceFormat("pdf").evidenceMimeType("application/pdf").evidenceSize(100L).build();
        when(repository.findByIdForUpdate(credId)).thenReturn(Optional.of(credential));

        var result = service.approve(credId, adminId, 0L);
        assertEquals(CredentialStatus.APPROVED, result.view().status());
        verify(auditTrail).append(eq(adminId), eq("TEACHER_CREDENTIAL_APPROVED"), eq("TEACHER_CREDENTIAL"), eq(credential.getId()), any(), any());
        verify(revalidationClient).revalidateTeacherPublicData(teacherId, "TEACHER_CREDENTIAL_APPROVED");
    }

    @Test
    void approve_concurrentModification_throws409() {
        UUID credId = UUID.randomUUID(), adminId = UUID.randomUUID();
        TeacherCredential credential = mock(TeacherCredential.class);
        when(credential.getVersion()).thenReturn(5L);
        when(repository.findByIdForUpdate(credId)).thenReturn(Optional.of(credential));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approve(credId, adminId, 4L));
        assertEquals(ErrorCode.CONCURRENT_MODIFICATION, ex.getErrorCode());
    }

    @Test
    void reject_requiresReason() {
        UUID credId = UUID.randomUUID(), adminId = UUID.randomUUID();
        TeacherCredential credential = mock(TeacherCredential.class);
        when(credential.getVersion()).thenReturn(0L);
        when(repository.findByIdForUpdate(credId)).thenReturn(Optional.of(credential));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.reject(credId, "   ", adminId, 0L));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void reject_recordsAudit() {
        UUID credId = UUID.randomUUID(), adminId = UUID.randomUUID(), teacherId = UUID.randomUUID();
        TeacherProfile profile = mock(TeacherProfile.class);
        when(profile.getId()).thenReturn(teacherId);

        TeacherCredential credential = TeacherCredential.builder()
                .teacher(profile).label("Degree").evidencePublicId("p").evidenceResourceType("auto")
                .evidenceFormat("pdf").evidenceMimeType("application/pdf").evidenceSize(100L).build();
        when(repository.findByIdForUpdate(credId)).thenReturn(Optional.of(credential));

        var result = service.reject(credId, "Illegible document", adminId, 0L);
        assertEquals(CredentialStatus.REJECTED, result.view().status());
        verify(auditTrail).append(eq(adminId), eq("TEACHER_CREDENTIAL_REJECTED"), eq("TEACHER_CREDENTIAL"), eq(credential.getId()), any(), any());
    }

    @Test
    void updateAndDeleted_whenWasApproved_triggersRevokeRevalidation() {
        UUID user = UUID.randomUUID(), teacherId = UUID.randomUUID(), credId = UUID.randomUUID();
        TeacherProfile profile = mock(TeacherProfile.class);
        when(profile.getId()).thenReturn(teacherId);
        when(profiles.findByUserId(user)).thenReturn(Optional.of(profile));

        TeacherCredential credential = TeacherCredential.builder()
                .teacher(profile).label("Old").evidencePublicId("p").evidenceResourceType("auto")
                .evidenceFormat("pdf").evidenceMimeType("application/pdf").evidenceSize(100L).build();
        credential.approve(UUID.randomUUID()); // previously approved!
        when(repository.findById(credId)).thenReturn(Optional.of(credential));

        service.update(user, credId, "Renamed Label", null, 0L);
        verify(revalidationClient).revalidateTeacherPublicData(teacherId, "TEACHER_CREDENTIAL_REVOKED");

        // Now test delete when was approved
        credential.approve(UUID.randomUUID());
        service.delete(user, credId, 0L);
        verify(revalidationClient, times(2)).revalidateTeacherPublicData(teacherId, "TEACHER_CREDENTIAL_REVOKED");
    }
}
