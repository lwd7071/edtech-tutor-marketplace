package com.edtech.platform.teacher.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.storage.FileStoragePort;
import com.edtech.platform.teacher.domain.DocumentType;
import com.edtech.platform.teacher.domain.TeacherDocument;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.domain.VerificationStatus;
import com.edtech.platform.teacher.dto.TeacherDocumentView;
import com.edtech.platform.teacher.repository.TeacherDocumentRepository;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherDocumentServiceTest {

    @Mock
    private TeacherDocumentRepository teacherDocumentRepository;

    @Mock
    private TeacherProfileRepository teacherProfileRepository;

    @Mock
    private FileStoragePort fileStoragePort;

    @InjectMocks
    private TeacherDocumentService teacherDocumentService;

    private UUID userId;
    private UUID teacherProfileId;
    private TeacherProfile teacherProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teacherProfileId = UUID.randomUUID();
        teacherProfile = mock(TeacherProfile.class);
    }

    @Test
    void getDocuments_success() {
        when(teacherProfile.getId()).thenReturn(teacherProfileId);
        when(teacherProfileRepository.findByUserId(userId)).thenReturn(Optional.of(teacherProfile));

        TeacherDocument doc = mock(TeacherDocument.class);
        when(doc.getId()).thenReturn(UUID.randomUUID());
        when(doc.getDocumentType()).thenReturn(DocumentType.DEGREE);
        when(doc.getTitle()).thenReturn("Degree");
        when(doc.getSecureUrl()).thenReturn("https://cloudinary.com/doc.pdf");
        when(doc.getMimeType()).thenReturn("application/pdf");
        when(doc.getFileSize()).thenReturn(100L);
        when(doc.getVerificationStatus()).thenReturn(VerificationStatus.PENDING);

        when(teacherDocumentRepository.findByTeacherIdIn(List.of(teacherProfileId))).thenReturn(List.of(doc));

        List<TeacherDocumentView> result = teacherDocumentService.getDocuments(userId);

        assertEquals(1, result.size());
        assertEquals("Degree", result.get(0).title());
    }

    @Test
    void getDocuments_profileNotFound_throwsException() {
        when(teacherProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherDocumentService.getDocuments(userId));
        assertEquals(ErrorCode.TEACHER_PROFILE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void uploadDocument_success() throws Exception {
        when(teacherProfile.getId()).thenReturn(teacherProfileId);
        when(teacherProfileRepository.findByUserId(userId)).thenReturn(Optional.of(teacherProfile));

        // 1x1 transparent PNG magic bytes
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "cert.png", "image/png", pngBytes);

        FileStoragePort.UploadResult uploadResult = new FileStoragePort.UploadResult(
                "teacher_documents/" + teacherProfileId + "/sample",
                "https://res.cloudinary.com/demo/image/upload/v1/sample.png",
                "image/png",
                (long) pngBytes.length
        );

        when(fileStoragePort.upload(eq(file), eq("teacher_documents/" + teacherProfileId)))
                .thenReturn(uploadResult);

        TeacherDocument savedDoc = mock(TeacherDocument.class);
        when(savedDoc.getId()).thenReturn(UUID.randomUUID());
        when(savedDoc.getDocumentType()).thenReturn(DocumentType.CERTIFICATE);
        when(savedDoc.getTitle()).thenReturn("Toeic Cert");
        when(savedDoc.getSecureUrl()).thenReturn(uploadResult.secureUrl());
        when(savedDoc.getMimeType()).thenReturn(uploadResult.mimeType());
        when(savedDoc.getFileSize()).thenReturn(uploadResult.fileSize());
        when(savedDoc.getVerificationStatus()).thenReturn(VerificationStatus.PENDING);

        when(teacherDocumentRepository.save(any(TeacherDocument.class))).thenReturn(savedDoc);

        TeacherDocumentView view = teacherDocumentService.uploadDocument(userId, file, DocumentType.CERTIFICATE, "Toeic Cert");

        assertNotNull(view);
        assertEquals("Toeic Cert", view.title());
        assertEquals(uploadResult.secureUrl(), view.secureUrl());
        verify(fileStoragePort).upload(eq(file), eq("teacher_documents/" + teacherProfileId));
        verify(teacherDocumentRepository).save(any(TeacherDocument.class));
    }

    @Test
    void uploadDocument_emptyFile_throwsValidation() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherDocumentService.uploadDocument(userId, emptyFile, DocumentType.DEGREE, "Title"));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void uploadDocument_unsupportedFileType_throwsFileTypeNotAllowed() {
        byte[] txtBytes = "plain text content".getBytes();
        MockMultipartFile txtFile = new MockMultipartFile("file", "note.txt", "text/plain", txtBytes);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherDocumentService.uploadDocument(userId, txtFile, DocumentType.DEGREE, "Title"));
        assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void uploadDocument_profileNotFound_throwsProfileNotFound() {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "cert.png", "image/png", pngBytes);

        when(teacherProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherDocumentService.uploadDocument(userId, file, DocumentType.DEGREE, "Title"));
        assertEquals(ErrorCode.TEACHER_PROFILE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void deleteDocument_success() throws Exception {
        UUID docId = UUID.randomUUID();
        TeacherDocument doc = mock(TeacherDocument.class);
        when(doc.getTeacher()).thenReturn(teacherProfile);
        when(teacherProfile.getUserId()).thenReturn(userId);
        when(doc.getVerificationStatus()).thenReturn(VerificationStatus.PENDING);
        when(doc.getCloudinaryPublicId()).thenReturn("public-id-123");

        when(teacherDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));

        teacherDocumentService.deleteDocument(userId, docId);

        verify(fileStoragePort).delete("public-id-123");
        verify(teacherDocumentRepository).delete(doc);
    }

    @Test
    void deleteDocument_notFound_throwsException() {
        UUID docId = UUID.randomUUID();
        when(teacherDocumentRepository.findById(docId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherDocumentService.deleteDocument(userId, docId));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void deleteDocument_forbiddenUser_throwsForbidden() {
        UUID docId = UUID.randomUUID();
        TeacherDocument doc = mock(TeacherDocument.class);
        when(doc.getTeacher()).thenReturn(teacherProfile);
        when(teacherProfile.getUserId()).thenReturn(UUID.randomUUID()); // different user

        when(teacherDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherDocumentService.deleteDocument(userId, docId));
        assertEquals(ErrorCode.FORBIDDEN_RESOURCE, ex.getErrorCode());
    }

    @Test
    void deleteDocument_verifiedDocument_throwsNotDeletable() {
        UUID docId = UUID.randomUUID();
        TeacherDocument doc = mock(TeacherDocument.class);
        when(doc.getTeacher()).thenReturn(teacherProfile);
        when(teacherProfile.getUserId()).thenReturn(userId);
        when(doc.getVerificationStatus()).thenReturn(VerificationStatus.VERIFIED);

        when(teacherDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherDocumentService.deleteDocument(userId, docId));
        assertEquals(ErrorCode.TEACHER_DOCUMENT_NOT_DELETABLE, ex.getErrorCode());
    }
}
