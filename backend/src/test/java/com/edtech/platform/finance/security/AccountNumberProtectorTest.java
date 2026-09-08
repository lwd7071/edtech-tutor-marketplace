package com.edtech.platform.finance.security;

import com.edtech.platform.common.config.properties.AccountEncryptionProperties;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class AccountNumberProtectorTest {
    private final AccountNumberProtector protector = new AccountNumberProtector(
            new AccountEncryptionProperties("01234567890123456789012345678901"), new SecureRandom());

    @Test
    void encryptUsesRandomIvAndRoundTrips() {
        String raw = "123456789012";
        String first = protector.encrypt(raw);
        String second = protector.encrypt(raw);
        assertNotEquals(first, second);
        assertEquals(raw, protector.decrypt(first));
        assertFalse(first.contains(raw));
    }

    @Test
    void tamperingIsRejected() {
        String encrypted = protector.encrypt("123456789");
        char last = encrypted.charAt(encrypted.length() - 1);
        String tampered = encrypted.substring(0, encrypted.length() - 1) + (last == 'A' ? 'B' : 'A');
        assertThrows(RuntimeException.class, () -> protector.decrypt(tampered));
    }

    @Test
    void maskKeepsOnlyLastFourDigits() {
        assertEquals("******6789", protector.mask("123456789"));
    }
}
