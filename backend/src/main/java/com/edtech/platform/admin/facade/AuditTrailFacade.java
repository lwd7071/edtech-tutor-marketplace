package com.edtech.platform.admin.facade;

import java.util.Map;
import java.util.UUID;

/** Neutral boundary for other modules to append an audit record in their transaction. */
public interface AuditTrailFacade {
    void append(UUID actorId, String action, String targetType, UUID targetId,
                Map<String, Object> before, Map<String, Object> after);
}
