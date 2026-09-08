package com.edtech.platform.finance.security;

import com.edtech.platform.common.config.properties.AccountEncryptionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class AccountNumberProtector {
    private final AccountEncryptionProperties properties;
    private final SecureRandom random;

    public String encrypt(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, iv);
            byte[] encrypted = cipher.doFinal(raw.trim().getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return "v1:" + Base64.getEncoder().encodeToString(payload);
        } catch (Exception failure) {
            throw new IllegalStateException("account encryption failed", failure);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank() || !encrypted.startsWith("v1:")) return encrypted;
        try {
            byte[] payload = Base64.getDecoder().decode(encrypted.substring(3));
            byte[] iv = Arrays.copyOfRange(payload, 0, 12);
            byte[] data = Arrays.copyOfRange(payload, 12, payload.length);
            return new String(cipher(Cipher.DECRYPT_MODE, iv).doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception failure) {
            throw new IllegalArgumentException("invalid encrypted account number", failure);
        }
    }

    public String mask(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String value = raw.trim();
        return value.length() <= 4 ? value : "******" + value.substring(value.length() - 4);
    }

    private Cipher cipher(int mode, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(properties.key().getBytes(StandardCharsets.UTF_8), "AES"),
                new GCMParameterSpec(128, iv));
        return cipher;
    }
}
