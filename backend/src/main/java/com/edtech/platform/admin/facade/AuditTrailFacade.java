package com.edtech.platform.admin.facade;

import java.util.Map;
import java.util.UUID;

/**
 * Neutral boundary for other modules to append an audit record in their transaction.
 * Snapshot maps are copied at the audit boundary; nullable values are preserved,
 * keys must be non-null, and only the outer map is copied.
 */
public interface AuditTrailFacade {
    void append(UUID actorId, String action, String targetType, UUID targetId,
                Map<String, Object> before, Map<String, Object> after);
}
