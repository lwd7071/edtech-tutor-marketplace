package com.edtech.platform.booking.controller;

import com.edtech.platform.booking.dto.request.CancelBookingRequest;
import com.edtech.platform.booking.dto.request.CompleteBookingRequest;
import com.edtech.platform.booking.dto.request.CreateBookingRequest;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/bookings")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherBookingController {

    private final BookingService service;
    private final BookingReadService reads;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> create(@AuthenticationPrincipal AuthenticatedUser u, @Valid @RequestBody CreateBookingRequest r) {
        BookingDetail booking = service.create(u.id(), r);
        return ApiResponse.created(reads.detail(u.id(), booking.id(), true));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<?> complete(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable UUID id, @Valid @RequestBody CompleteBookingRequest r) {
        service.complete(u.id(), id, r);
        return ApiResponse.ok(reads.detail(u.id(), id, true));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<?> cancel(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable UUID id, @Valid @RequestBody CancelBookingRequest r) {
        service.cancel(u.id(), id, r);
        return ApiResponse.ok(reads.detail(u.id(), id, true));
    }

    public record DisputeRequest(@Min(0) long version, @NotBlank String reason) {}

    @PostMapping("/{id}/dispute")
    public ApiResponse<?> dispute(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable UUID id,
                                  @Valid @RequestBody DisputeRequest request) {
        service.dispute(u.id(), id, request.version(), request.reason());
        return ApiResponse.ok(reads.detail(u.id(), id, true));
    }
}
