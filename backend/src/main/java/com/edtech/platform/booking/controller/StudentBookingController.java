package com.edtech.platform.booking.controller;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/student/bookings")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentBookingController {
    private final BookingReadService service;
    private final BookingService bookingService;
    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required=false) BookingStatus status,@RequestParam(required=false) Instant from,
            @RequestParam(required=false) Instant to,Pageable pageable){
        var p=service.list(user.id(),false,status,from,to,pageable);
        return ApiResponse.page(p.getContent(),PageMeta.from(p));
    }

    public record ConfirmBookingRequest(@Min(0) long version) {}

    @PostMapping("/{id}/confirm")
    public ApiResponse<?> confirm(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable java.util.UUID id,
                                  @Valid @RequestBody ConfirmBookingRequest request) {
        bookingService.confirmByStudent(user.id(), id, request.version());
        return ApiResponse.ok(service.detail(user.id(), id, false));
    }
}

