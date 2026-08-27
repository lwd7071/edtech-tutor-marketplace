package com.edtech.platform.auth.facade;

import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.Map;

public interface IdentityFacade {
    boolean existsById(UUID id);
    boolean isActive(UUID id);
    void evictUser(UUID userId);
    java.util.Set<UUID> searchUserIdsByKeyword(String keyword);
    Optional<IdentitySnapshot> getIdentity(UUID id);
    Map<UUID, IdentitySnapshot> getIdentities(Collection<UUID> ids);
    Optional<String> getStatus(UUID id);
}
