package com.edtech.platform.auth.service;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.dto.request.UpdateParentContactRequest;
import com.edtech.platform.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ParentContactServiceTest {
    @Test
    void removingEmailAlsoDisablesParentEmailNotifications() {
        UUID studentId = UUID.randomUUID();
        User user = User.builder().email("student@example.test").passwordHash("hash").fullName("Student")
                .role(Role.STUDENT).status(UserStatus.ACTIVE).emailVerified(true)
                .parentEmail("parent@example.test").notifyParent(true).build();
        UserRepository users = mock(UserRepository.class);
        when(users.findById(studentId)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);

        var result = new ParentContactService(users).update(studentId,
                new UpdateParentContactRequest("Parent", "0900000000", "  ", true));

        assertThat(result.parentEmail()).isNull();
        assertThat(result.notifyParent()).isFalse();
    }
}
