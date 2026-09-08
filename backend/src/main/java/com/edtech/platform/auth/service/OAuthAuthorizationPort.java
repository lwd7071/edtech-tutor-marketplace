package com.edtech.platform.auth.service;

public interface OAuthAuthorizationPort {
    OAuthAuthorizationResult authorize(OAuthIdentity identity);
}
