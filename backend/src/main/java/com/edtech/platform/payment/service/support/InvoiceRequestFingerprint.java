package com.edtech.platform.payment.service.support;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/** Builds the stable request fingerprint used by invoice idempotency checks. */
public final class InvoiceRequestFingerprint {

    public String canonicalize(
            UUID studentId,
            UUID pricingPackageId,
            long amountVnd,
            String returnUrl,
            String cancelUrl) {
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(pricingPackageId, "pricingPackageId");
        if (amountVnd <= 0) {
            throw new IllegalArgumentException("amountVnd must be positive");
        }

        String canonicalReturnUrl = canonicalHttpUrl(returnUrl, "returnUrl");
        String canonicalCancelUrl = canonicalHttpUrl(cancelUrl, "cancelUrl");
        int returnUrlBytes = canonicalReturnUrl.getBytes(StandardCharsets.UTF_8).length;
        int cancelUrlBytes = canonicalCancelUrl.getBytes(StandardCharsets.UTF_8).length;

        return new StringBuilder(256)
                .append("studentId=").append(studentId).append('\n')
                .append("pricingPackageId=").append(pricingPackageId).append('\n')
                .append("amountVnd=").append(amountVnd).append('\n')
                .append("returnUrlBytes=").append(returnUrlBytes).append('\n')
                .append("returnUrl=").append(canonicalReturnUrl).append('\n')
                .append("cancelUrlBytes=").append(cancelUrlBytes).append('\n')
                .append("cancelUrl=").append(canonicalCancelUrl)
                .toString();
    }

    public String sha256(
            UUID studentId,
            UUID pricingPackageId,
            long amountVnd,
            String returnUrl,
            String cancelUrl) {
        byte[] canonicalBytes = canonicalize(studentId, pricingPackageId, amountVnd, returnUrl, cancelUrl)
                .getBytes(StandardCharsets.UTF_8);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalBytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String canonicalHttpUrl(String rawUrl, String field) {
        Objects.requireNonNull(rawUrl, field);
        try {
            URI uri = new URI(rawUrl);
            String scheme = uri.getScheme();
            if (!uri.isAbsolute()
                    || scheme == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException(field + " must be an absolute HTTP(S) URL");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(field + " must be a valid URL", exception);
        }
    }
}
