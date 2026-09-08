package com.edtech.platform.learning.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.facade.AttachmentFacade;
import com.edtech.platform.learning.dto.response.ContentBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AssignmentAttachmentBinder {
    private final AttachmentFacade attachments;

    public void bind(UUID ownerId, String attachableType, List<ContentBlock> blocks, UUID attachableId) {
        if (blocks == null) return;
        for (ContentBlock block : blocks) {
            if ("IMAGE".equals(block.getType()) || "FILE".equals(block.getType())) {
                if (block.getAttachmentId() == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "Missing attachmentId for IMAGE/FILE block");
                }
                attachments.validateAndBind(block.getAttachmentId(), ownerId, attachableType, attachableId);
            }
        }
    }
}
