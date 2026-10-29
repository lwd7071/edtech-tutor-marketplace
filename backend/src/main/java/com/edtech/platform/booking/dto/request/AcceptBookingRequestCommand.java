package com.edtech.platform.booking.dto.request;

import com.edtech.platform.booking.domain.DeliveryMode;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AcceptBookingRequestCommand(
        @NotNull @Future Instant startTime,
        @NotNull DeliveryMode deliveryMode,
        @Size(max = 500) String meetingLink,
        @Size(max = 500) String locationAddress,
        @Min(0) long version
) {}
