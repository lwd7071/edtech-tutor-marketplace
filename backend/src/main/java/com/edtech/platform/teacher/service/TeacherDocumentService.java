package com.edtech.platform.teacher.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.storage.FileStoragePort;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.teacher.domain.DocumentType;
import com.edtech.platform.teacher.domain.TeacherDocument;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.domain.VerificationStatus;
import com.edtech.platform.teacher.dto.TeacherDocumentView;
import com.edtech.platform.teacher.repository.TeacherDocumentRepository;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherDocumentService {

    private final TeacherDocumentRepository teacherDocumentRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final FileStoragePort fileStoragePort;
    private final CredentialProofStorage privateStorage;
    private final Tika tika = new Tika();

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Transactional(readOnly = true)
    public List<TeacherDocumentView> getDocuments(UUID userId) {
        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));
        return teacherDocumentRepository.findByTeacherIdIn(List.of(profile.getId())).stream().map(this::toView).toList();
    }
    private static final List<String> ALLOWED_MIME_TYPES = List.of("image/jpeg", "image/png", "application/pdf");

    @Transactional
    public TeacherDocumentView uploadDocument(UUID userId, MultipartFile file, DocumentType documentType, String title) {
        if (documentType != DocumentType.IDENTITY) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Tài liệu xác minh chỉ nhận giấy tờ định danh");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tệp tải lên không được để trống");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        try {
            String mimeType = tika.detect(file.getInputStream());
            if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
            }

            TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

            CredentialProofStorage.StoredFile result = privateStorage.upload(file, "teacher_documents/" + profile.getId());
            cleanupOnRollback(result.publicId());

            TeacherDocument document = TeacherDocument.builder()
                    .teacher(profile)
                    .documentType(documentType)
                    .title(title)
                    .cloudinaryPublicId(result.publicId())
                    .secureUrl(result.secureUrl())
                    .mimeType(mimeType)
                    .fileSize(file.getSize())
                    .build();

            return toView(teacherDocumentRepository.save(document));

        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public CredentialProofStorage.DownloadedFile adminProof(UUID documentId) throws IOException {
        TeacherDocument document = teacherDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        String storedUrl = document.getSecureUrl();
        if (storedUrl != null && storedUrl.contains("/authenticated/")) {
            return privateStorage.download(document.getCloudinaryPublicId());
        }
        return privateStorage.downloadLegacy(document.getCloudinaryPublicId());
    }

    private void cleanupOnRollback(String publicId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try { privateStorage.delete(publicId); }
                    catch (IOException e) { log.warn("Failed to clean up private document", e); }
                }
            }
        });
    }

    @Transactional
    public void deleteDocument(UUID userId, UUID documentId) {
        TeacherDocument document = teacherDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!document.getTeacher().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE);
        }

        if (document.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new BusinessException(ErrorCode.TEACHER_DOCUMENT_NOT_DELETABLE);
        }

        try {
            if (document.getCloudinaryPublicId() != null) {
                if (document.getSecureUrl() != null && document.getSecureUrl().contains("/authenticated/")) {
                    privateStorage.delete(document.getCloudinaryPublicId());
                } else {
                    fileStoragePort.delete(document.getCloudinaryPublicId());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to delete file from Cloudinary: {}", document.getCloudinaryPublicId(), e);
        }

        teacherDocumentRepository.delete(document);
    }

    private TeacherDocumentView toView(TeacherDocument document) {
        return new TeacherDocumentView(
                document.getId(),
                document.getDocumentType(),
                document.getTitle(),
                document.getSecureUrl(),
                document.getMimeType(),
                document.getFileSize(),
                document.getVerificationStatus(),
                document.getVerifiedAt()
        );
    }
}
