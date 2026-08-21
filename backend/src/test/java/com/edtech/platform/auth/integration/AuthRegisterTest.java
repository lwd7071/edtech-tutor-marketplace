package com.edtech.platform.auth.integration;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.dto.request.RegisterRequest;
import com.edtech.platform.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AuthRegisterTest extends AuthIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerStudentWithParentEmail_setsNotifyParentTrue() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "student1@example.com",
                "Password123!",
                "Student One",
                Role.STUDENT,
                "Parent One",
                "1234567890",
                "parent1@example.com"
        );

        // Act
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Assert
        User savedUser = userRepository.findByEmailIgnoreCase("student1@example.com").orElseThrow();
        assertThat(savedUser.getNotifyParent()).isTrue();
    }
}
