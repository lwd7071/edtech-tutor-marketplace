package com.edtech.platform.booking.dto.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
public record RejectTrialRequest(String reason, @NotNull @Min(0) Long version) {
    public RejectTrialRequest(String reason) { this(reason, 0L); }
}
