package com.edtech.platform.booking.dto.request;

import jakarta.validation.constraints.Min;

public record CancelBookingRequestCommand(@Min(0) long version) {}
