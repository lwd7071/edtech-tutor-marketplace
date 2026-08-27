package com.edtech.platform.auth.facade.impl;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

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
    public void evictUser(UUID userId) {
        // Dummy implementation since eviction isn't handled here
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Set<UUID> searchUserIdsByKeyword(String keyword) {
        return userRepository.searchIdsByKeyword(keyword);
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
                        user.getRole() != null ? user.getRole().name() : null,
                        user.getStatus() != null ? user.getStatus().name() : null,
                        user.getAvatarUrl(),
                        user.getNotifyParent(),
                        user.getParentEmail()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, IdentitySnapshot> getIdentities(Collection<UUID> ids) {
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(
                user -> user.getId(),
                user -> new IdentitySnapshot(user.getId(), user.getEmail(), user.getFullName(),
                        user.getRole() != null ? user.getRole().name() : null,
                        user.getStatus() != null ? user.getStatus().name() : null,
                        user.getAvatarUrl(), user.getNotifyParent(), user.getParentEmail())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getStatus(UUID id) {
        return userRepository.findById(id).map(user -> user.getStatus().name());
    }
}
