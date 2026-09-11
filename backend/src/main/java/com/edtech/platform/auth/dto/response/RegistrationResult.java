package com.edtech.platform.auth.dto.response;

public record RegistrationResult(String email, boolean verificationRequired) {
}
