package com.edtech.platform.common.facade.impl;

import com.edtech.platform.common.domain.AttachableType;
import com.edtech.platform.common.domain.Attachment;
import com.edtech.platform.common.dto.response.AttachmentView;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.facade.AttachmentFacade;
import com.edtech.platform.common.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentFacadeImpl implements AttachmentFacade {

    private final AttachmentRepository attachmentRepository;

    @Override
    @Transactional
    public void validateAndBind(UUID attachmentId, UUID ownerId, String expectedType, UUID attachableId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));

        if (!attachment.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.ATTACHMENT_CONTEXT_INVALID);
        }

        if (!attachment.getAttachableType().name().equals(expectedType)) {
            throw new BusinessException(ErrorCode.ATTACHMENT_CONTEXT_INVALID);
        }

        // Idempotency: If already bound to the exact same attachableId, do nothing
        if (attachment.getAttachableId().equals(attachableId)) {
            return;
        }

        // A pending attachment is self-referencing (attachableId == id)
        if (!attachment.getAttachableId().equals(attachment.getId())) {
            throw new BusinessException(ErrorCode.ATTACHMENT_CONTEXT_INVALID);
        }

        attachment.setAttachableId(attachableId);
        attachmentRepository.save(attachment);
    }

    @Override
    public AttachmentView getAttachmentView(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));

        return new AttachmentView(
                attachment.getId(),
                attachment.getSecureUrl(),
                attachment.getOriginalFilename(),
                attachment.getMimeType(),
                attachment.getFileSize()
        );
    }
}
