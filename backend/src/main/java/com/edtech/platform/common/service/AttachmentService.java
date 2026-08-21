package com.edtech.platform.common.service;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.common.domain.AttachableType;
import com.edtech.platform.common.domain.Attachment;
import com.edtech.platform.common.dto.response.AttachmentView;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.repository.AttachmentRepository;
import com.edtech.platform.common.storage.FileStoragePort;
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
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final FileStoragePort fileStoragePort;
    private final Tika tika = new Tika();

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // docx
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // xlsx
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", // pptx
            "application/zip",
            "application/x-zip-compressed",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    @Transactional
    public AttachmentView uploadAttachment(UUID ownerId, AttachableType attachableType, MultipartFile file) {
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

            User owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

            FileStoragePort.UploadResult result = fileStoragePort.upload(file, "attachments/" + attachableType.name().toLowerCase() + "/" + ownerId);

            Attachment attachment = Attachment.builder()
                    .owner(owner)
                    .attachableType(attachableType)
                    .attachableId(UUID.randomUUID()) // Temporary value to satisfy NOT NULL constraint
                    .cloudinaryPublicId(result.publicId())
                    .secureUrl(result.secureUrl())
                    .originalFilename(file.getOriginalFilename())
                    .mimeType(mimeType)
                    .fileSize(file.getSize())
                    .build();
            
            // Save to get the actual ID
            attachment = attachmentRepository.saveAndFlush(attachment);
            
            // Self-reference for pending attachment
            attachment.setAttachableId(attachment.getId());

            return toView(attachment);

        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private AttachmentView toView(Attachment attachment) {
        return new AttachmentView(
                attachment.getId(),
                attachment.getSecureUrl(),
                attachment.getOriginalFilename(),
                attachment.getMimeType(),
                attachment.getFileSize()
        );
    }
}
