package com.edtech.platform.admin.dto.request;

import java.time.Instant;

public record CompleteTransferRequest(
        String bankReference,
        Instant transferredAt,
        String proofPublicId,
        String proofUrl,
        long version
) {}