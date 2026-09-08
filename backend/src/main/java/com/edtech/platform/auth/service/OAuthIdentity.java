package com.edtech.platform.auth.service;

public record OAuthIdentity(String provider, String subject, String email, String fullName) {
}
