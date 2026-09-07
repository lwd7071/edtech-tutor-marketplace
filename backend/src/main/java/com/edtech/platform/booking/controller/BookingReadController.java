package com.edtech.platform.booking.controller;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class BookingReadController {
    private final BookingReadService service;
    @GetMapping("/api/teacher/bookings")
    @RequireRole("TEACHER")
    public ApiResponse<?> teacherList(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required=false) BookingStatus status,@RequestParam(required=false) Instant from,
            @RequestParam(required=false) Instant to,Pageable pageable){
        var p=service.list(user.id(),true,status,from,to,pageable);
        return ApiResponse.page(p.getContent(),PageMeta.from(p));
    }
    @GetMapping("/api/student/bookings/{id}")
    @RequireRole("STUDENT")
    public ApiResponse<?> studentDetail(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id){
        return ApiResponse.ok(service.detail(user.id(),id,false));
    }
    @GetMapping("/api/teacher/bookings/{id}")
    @RequireRole("TEACHER")
    public ApiResponse<?> teacherDetail(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id){
        return ApiResponse.ok(service.detail(user.id(),id,true));
    }
}

