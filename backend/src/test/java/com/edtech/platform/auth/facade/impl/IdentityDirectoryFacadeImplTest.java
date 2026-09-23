package com.edtech.platform.auth.facade.impl;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IdentityDirectoryFacadeImplTest {
    @Test
    void listsOnlyEligibleRolesAndStatusesThroughAuthRepository() {
        UserRepository users = mock(UserRepository.class);
        IdentityDirectoryFacadeImpl facade = new IdentityDirectoryFacadeImpl(users);
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-01T00:00:00Z");
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getFullName()).thenReturn("Học viên");
        when(user.getEmail()).thenReturn("student@example.test");
        when(user.getRole()).thenReturn(Role.STUDENT);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getCreatedAt()).thenReturn(createdAt);
        when(user.getLastLoginAt()).thenReturn(null);
        var pageable = PageRequest.of(0, 20);
        when(users.findDirectory(List.of(Role.STUDENT, Role.TEACHER), List.of(UserStatus.ACTIVE, UserStatus.LOCKED),
                null, null, "student", pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        var result = facade.searchUsers("student", null, null, pageable);

        assertThat(result.getContent()).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.id()).isEqualTo(id);
            assertThat(snapshot.fullName()).isEqualTo("Học viên");
            assertThat(snapshot.lastLoginAt()).isNull();
        });
        verify(users).findDirectory(List.of(Role.STUDENT, Role.TEACHER), List.of(UserStatus.ACTIVE, UserStatus.LOCKED),
                null, null, "student", pageable);
    }
}
