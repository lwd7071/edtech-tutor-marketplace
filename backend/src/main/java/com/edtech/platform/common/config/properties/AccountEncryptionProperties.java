package com.edtech.platform.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.finance.account-encryption")
public record AccountEncryptionProperties(String key) {
    public AccountEncryptionProperties {
        if (key == null || key.length() != 32) {
            throw new IllegalArgumentException("app.finance.account-encryption.key must be exactly 32 characters");
        }
    }
}
