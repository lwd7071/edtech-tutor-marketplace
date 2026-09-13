package com.edtech.platform.booking.dto.request;
import com.edtech.platform.booking.domain.DeliveryMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
public record AcceptTrialRequest(Instant startTime, Instant endTime, DeliveryMode deliveryMode,
                                 String meetingLink, String locationAddress,
                                 @NotNull @Min(0) Long version) {
    public AcceptTrialRequest(Instant startTime, Instant endTime, DeliveryMode deliveryMode,
                              String meetingLink, String locationAddress) {
        this(startTime, endTime, deliveryMode, meetingLink, locationAddress, 0L);
    }
}
