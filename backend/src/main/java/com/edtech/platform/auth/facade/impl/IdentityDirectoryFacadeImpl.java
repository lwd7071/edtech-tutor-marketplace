package com.edtech.platform.auth.facade.impl;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.facade.IdentityDirectoryFacade;
import com.edtech.platform.auth.facade.dto.UserDirectorySnapshot;
import com.edtech.platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IdentityDirectoryFacadeImpl implements IdentityDirectoryFacade {
    private final UserRepository users;

    @Override
    @Transactional(readOnly = true)
    public Page<UserDirectorySnapshot> searchUsers(String keyword, String role, String status, Pageable pageable) {
        Role parsedRole = role == null ? null : Role.valueOf(role);
        UserStatus parsedStatus = status == null ? null : UserStatus.valueOf(status);
        return users.findDirectory(List.of(Role.STUDENT, Role.TEACHER), List.of(UserStatus.ACTIVE, UserStatus.LOCKED),
                        parsedRole, parsedStatus, keyword, pageable)
                .map(IdentityDirectoryFacadeImpl::snapshot);
    }

    private static UserDirectorySnapshot snapshot(User user) {
        return new UserDirectorySnapshot(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(),
                user.getStatus().name(), user.getCreatedAt(), user.getLastLoginAt());
    }
}
