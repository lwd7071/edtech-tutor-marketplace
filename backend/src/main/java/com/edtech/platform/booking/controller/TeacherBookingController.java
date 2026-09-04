package com.edtech.platform.booking.controller;

import com.edtech.platform.booking.dto.request.CancelBookingRequest;
import com.edtech.platform.booking.dto.request.CompleteBookingRequest;
import com.edtech.platform.booking.dto.request.CreateBookingRequest;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import jakarta.validation.Valid;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingDetail> create(@AuthenticationPrincipal AuthenticatedUser u, @Valid @RequestBody CreateBookingRequest r) {
        return ApiResponse.created(service.create(u.id(), r));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<BookingDetail> complete(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable UUID id, @Valid @RequestBody CompleteBookingRequest r) {
        return ApiResponse.ok(service.complete(u.id(), id, r));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<BookingDetail> cancel(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable UUID id, @Valid @RequestBody CancelBookingRequest r) {
        return ApiResponse.ok(service.cancel(u.id(), id, r));
    }
}