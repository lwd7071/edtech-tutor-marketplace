package com.edtech.platform.finance.command;

import java.time.Instant;

public record CompleteTransferCommand(
        String bankReference,
        Instant transferredAt,
        String proofPublicId,
        String proofUrl,
        long version
) {
}
