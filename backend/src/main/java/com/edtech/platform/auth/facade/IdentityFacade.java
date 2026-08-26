package com.edtech.platform.auth.facade;

import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import java.util.Optional;
import java.util.UUID;

public interface IdentityFacade {
    boolean existsById(UUID id);
    boolean isActive(UUID id);
    void evictUser(UUID userId);
    java.util.Set<UUID> searchUserIdsByKeyword(String keyword);
    Optional<IdentitySnapshot> getIdentity(UUID id);
    Optional<String> getStatus(UUID id);
}
