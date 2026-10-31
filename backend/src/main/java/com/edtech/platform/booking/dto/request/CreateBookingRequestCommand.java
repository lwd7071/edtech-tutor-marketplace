package com.edtech.platform.booking.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateBookingRequestCommand(
        @NotNull UUID studentPackageId,
        @NotNull @Future Instant preferredStartTime,
        @Size(max = 500) String studentNote
) {}
