package com.edtech.platform.auth.dto.request;

import com.edtech.platform.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank 
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$", 
             message = "Password must be at least 8 characters long and contain at least one letter, one number and one special character")
    String password,
    @NotBlank String fullName,
    @NotNull Role role,
    String parentFullName,
    String parentPhone,
    String parentEmail
) {}
