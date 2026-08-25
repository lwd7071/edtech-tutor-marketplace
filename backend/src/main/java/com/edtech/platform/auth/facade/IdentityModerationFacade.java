package com.edtech.platform.auth.facade;

import com.edtech.platform.auth.facade.dto.IdentityModerationChange;

import java.util.UUID;

public interface IdentityModerationFacade {
    IdentityModerationChange changeStatus(UUID actorId, UUID targetUserId, String requestedStatus);
}
