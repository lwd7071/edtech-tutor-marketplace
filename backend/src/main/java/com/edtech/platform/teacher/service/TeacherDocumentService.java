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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final Tika tika = new Tika();

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_MIME_TYPES = List.of("image/jpeg", "image/png", "application/pdf");

    @Transactional
    public TeacherDocumentView uploadDocument(UUID userId, MultipartFile file, DocumentType documentType, String title) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File is empty");
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

            FileStoragePort.UploadResult result = fileStoragePort.upload(file, "teacher_documents/" + profile.getId());

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

    @Transactional
    public void deleteDocument(UUID userId, UUID documentId) {
        TeacherDocument document = teacherDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!document.getTeacher().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE);
        }

        if (document.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new BusinessException(ErrorCode.TEACHER_DOCUMENT_NOT_DELETABLE);
        }

        try {
            if (document.getCloudinaryPublicId() != null) {
                fileStoragePort.delete(document.getCloudinaryPublicId());
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
