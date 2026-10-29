package com.edtech.platform.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectBookingRequestCommand(
        @NotBlank @Size(max = 1000) String reason,
        @Min(0) long version
) {}
