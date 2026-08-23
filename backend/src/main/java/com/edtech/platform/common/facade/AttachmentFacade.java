package com.edtech.platform.common.facade;


import com.edtech.platform.common.dto.response.AttachmentView;

import java.util.UUID;

public interface AttachmentFacade {
    /**
     * Validates that the attachment belongs to the owner, has the expected type, and is pending (attachableId == attachmentId).
     * If valid, binds the attachment to the target attachableId.
     */
    void validateAndBind(UUID attachmentId, UUID ownerId, String expectedType, UUID attachableId);

    /**
     * Returns an immutable view of the attachment for public consumption.
     */
    AttachmentView getAttachmentView(UUID attachmentId);
}
