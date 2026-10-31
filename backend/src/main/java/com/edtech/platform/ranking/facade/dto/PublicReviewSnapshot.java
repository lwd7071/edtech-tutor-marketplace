package com.edtech.platform.ranking.facade.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public record PublicReviewSnapshot(
        UUID id,
        short rating,
        String comment,
        ZonedDateTime createdAt,
        String studentName,
        String studentAvatarUrl
) {}
