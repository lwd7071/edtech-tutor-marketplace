package com.edtech.platform.auth.facade.impl;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IdentityFacadeImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IdentityFacadeImpl identityFacade;

    private User testUser;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testUser = User.builder()
                .email("test@example.com")
                .fullName("Test User")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(testUser, "id", testId);
    }

    @Test
    void existsById_ReturnsTrue_WhenUserExists() {
        when(userRepository.existsById(testId)).thenReturn(true);
        assertTrue(identityFacade.existsById(testId));
    }

    @Test
    void isActive_ReturnsTrue_WhenUserExistsAndIsActive() {
        when(userRepository.findById(testId)).thenReturn(Optional.of(testUser));
        assertTrue(identityFacade.isActive(testId));
    }

    @Test
    void getIdentity_ReturnsSnapshot_WhenUserExists() {
        when(userRepository.findById(testId)).thenReturn(Optional.of(testUser));
        
        Optional<IdentitySnapshot> snapshot = identityFacade.getIdentity(testId);
        
        assertTrue(snapshot.isPresent());
        assertEquals("test@example.com", snapshot.get().email());
        assertEquals("Test User", snapshot.get().fullName());
        assertEquals("STUDENT", snapshot.get().roleName());
        assertEquals("ACTIVE", snapshot.get().statusName());
    }
}
