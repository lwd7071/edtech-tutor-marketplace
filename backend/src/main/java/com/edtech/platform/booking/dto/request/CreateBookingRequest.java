package com.edtech.platform.booking.dto.request;

import com.edtech.platform.booking.domain.DeliveryMode;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateBookingRequest(@NotNull UUID studentPackageId, @NotNull Instant startTime,
                                   @NotNull Instant endTime, @NotNull DeliveryMode deliveryMode,
                                   String meetingLink, String locationAddress) {}
