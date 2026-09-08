package com.edtech.platform.auth.service;

public sealed interface OAuthAuthorizationResult {
    record LoginExchange(String exchangeCode) implements OAuthAuthorizationResult {}
    record RegistrationRequired(String registrationToken) implements OAuthAuthorizationResult {}
    record Rejected(String errorCode) implements OAuthAuthorizationResult {}
}
