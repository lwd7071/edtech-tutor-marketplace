package com.edtech.platform.auth.dto.request;
import com.edtech.platform.auth.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record CompleteOAuthRegistrationRequest(@NotBlank String registrationToken, @NotNull Role role) {}
