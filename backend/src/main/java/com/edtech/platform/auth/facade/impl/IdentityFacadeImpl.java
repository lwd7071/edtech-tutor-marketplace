package com.edtech.platform.auth.facade.impl;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityFacadeImpl implements IdentityFacade {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return userRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(UUID id) {
        return userRepository.findById(id)
                .map(user -> user.getStatus() == com.edtech.platform.auth.domain.UserStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentitySnapshot> getIdentity(UUID id) {
        return userRepository.findById(id)
                .map(user -> new IdentitySnapshot(
                        user.getId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole(),
                        user.getStatus(),
                        user.getAvatarUrl(),
                        user.getNotifyParent(),
                        user.getParentEmail()
                ));
    }
}
