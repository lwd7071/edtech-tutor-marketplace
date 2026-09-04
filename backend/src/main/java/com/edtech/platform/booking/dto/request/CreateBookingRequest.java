package com.edtech.platform.booking.dto.request;
import com.edtech.platform.booking.domain.DeliveryMode; import java.time.Instant; import java.util.UUID;
public record CreateBookingRequest(UUID studentPackageId, Instant startTime, Instant endTime, DeliveryMode deliveryMode, String meetingLink, String locationAddress) {}
