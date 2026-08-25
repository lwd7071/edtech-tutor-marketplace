package com.edtech.platform.auth.facade;

import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import java.util.Optional;
import java.util.UUID;

public interface IdentityFacade {
    boolean existsById(UUID id);
    boolean isActive(UUID id);
    Optional<IdentitySnapshot> getIdentity(UUID id);
    Optional<String> getStatus(UUID id);
}
