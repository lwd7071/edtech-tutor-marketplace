package com.edtech.platform.mail;

import java.util.UUID;

public record OutboundMail(
        UUID id,
        String recipient,
        String subject,
        String body,
        int attempts
) {
}
