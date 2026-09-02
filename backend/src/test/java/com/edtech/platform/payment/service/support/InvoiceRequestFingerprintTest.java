package com.edtech.platform.payment.service.support;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceRequestFingerprintTest {
    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PACKAGE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void canonicalPayloadHasStableBytesAndGoldenSha256() {
        InvoiceRequestFingerprint fingerprint = new InvoiceRequestFingerprint();

        String canonical = fingerprint.canonicalize(
                STUDENT_ID,
                PACKAGE_ID,
                1_000_000L,
                "https://app.example/payment/résult",
                "https://app.example/payment/cancel");

        String expected = "studentId=11111111-1111-1111-1111-111111111111\n"
                + "pricingPackageId=22222222-2222-2222-2222-222222222222\n"
                + "amountVnd=1000000\n"
                + "returnUrlBytes=39\n"
                + "returnUrl=https://app.example/payment/r%C3%A9sult\n"
                + "cancelUrlBytes=34\n"
                + "cancelUrl=https://app.example/payment/cancel";

        assertThat(canonical).isEqualTo(expected);
        assertThat(canonical.getBytes(StandardCharsets.UTF_8)).containsExactly(expected.getBytes(StandardCharsets.UTF_8));
        assertThat(fingerprint.sha256(STUDENT_ID, PACKAGE_ID, 1_000_000L,
                "https://app.example/payment/résult", "https://app.example/payment/cancel"))
                .isEqualTo("8bc5b46a1dd8ed19ba07da6bd98e68c35a54663f1b94b71572dfb2392292e72a");
    }

    @Test
    void fingerprintChangesWhenDerivedAmountOrRequestFieldsChange() {
        InvoiceRequestFingerprint fingerprint = new InvoiceRequestFingerprint();
        String baseline = fingerprint.sha256(STUDENT_ID, PACKAGE_ID, 1_000_000L,
                "https://app.example/payment/result", "https://app.example/payment/cancel");

        assertThat(fingerprint.sha256(STUDENT_ID, PACKAGE_ID, 1_000_001L,
                "https://app.example/payment/result", "https://app.example/payment/cancel")).isNotEqualTo(baseline);
        assertThat(fingerprint.sha256(STUDENT_ID, UUID.randomUUID(), 1_000_000L,
                "https://app.example/payment/result", "https://app.example/payment/cancel")).isNotEqualTo(baseline);
        assertThat(fingerprint.sha256(STUDENT_ID, PACKAGE_ID, 1_000_000L,
                "https://app.example/payment/other", "https://app.example/payment/cancel")).isNotEqualTo(baseline);
        assertThat(fingerprint.sha256(STUDENT_ID, PACKAGE_ID, 1_000_000L,
                "https://app.example/payment/result", "https://app.example/payment/other")).isNotEqualTo(baseline);
    }

    @Test
    void rejectsNonHttpUrls() {
        InvoiceRequestFingerprint fingerprint = new InvoiceRequestFingerprint();

        assertThatThrownBy(() -> fingerprint.sha256(
                STUDENT_ID, PACKAGE_ID, 1_000_000L, "javascript:alert(1)", "https://app.example/cancel"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
