package com.edtech.platform.auth.controller;

import com.edtech.platform.auth.dto.request.UpdateParentContactRequest;
import com.edtech.platform.auth.dto.response.ParentContactResponse;
import com.edtech.platform.auth.service.ParentContactService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StudentParentContactControllerTest {

    @Mock
    private ParentContactService parentContacts;

    @InjectMocks
    private StudentParentContactController controller;

    private AuthenticatedUser mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new AuthenticatedUser(UUID.randomUUID(), "student@test.com", "STUDENT");
    }

    @Test
    void shouldUpdateParentContactSuccessfully() {
        UpdateParentContactRequest request = new UpdateParentContactRequest(
                "Parent Name", "0123456789", "parent@test.com", true
        );
        ParentContactResponse expectedResponse = new ParentContactResponse(
                "Parent Name", "0123456789", "parent@test.com", true, Instant.now()
        );

        when(parentContacts.update(eq(mockUser.id()), any())).thenReturn(expectedResponse);

        ApiResponse<ParentContactResponse> response = controller.updateParentContact(mockUser, request);

        org.junit.jupiter.api.Assertions.assertTrue(response.success());
        assertEquals(expectedResponse, response.data());
    }

    @Test
    void shouldReadCurrentParentContact() {
        ParentContactResponse expected = new ParentContactResponse(null, null, "parent@test.com", true, Instant.now());
        when(parentContacts.get(mockUser.id())).thenReturn(expected);
        assertEquals(expected, controller.getParentContact(mockUser).data());
    }
}
