package com.edtech.platform.booking.controller;

import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.common.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.edtech.platform.booking.domain.SettlementStatus;
import com.edtech.platform.common.response.PageMeta;

@RestController
@RequestMapping("/api/admin/booking-settlements")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class AdminBookingSettlementController {
    private final BookingService service;
    private final BookingReadService reads;
    public record ActionRequest(@Min(0) long version, @NotBlank String note) {}

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required=false) SettlementStatus status, Pageable pageable) {
        var page = reads.adminSettlements(status, pageable);
        return ApiResponse.page(page.getContent(), PageMeta.from(page));
    }

    @PostMapping("/{id}/reopen")
    public ApiResponse<?> reopen(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody ActionRequest request) {
        service.reopenSettlement(admin.id(), id, request.version(), request.note());
        return ApiResponse.ok(reads.adminDetail(id));
    }

    @PostMapping("/{id}/release")
    public ApiResponse<?> release(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody ActionRequest request) {
        service.adminDecision(admin.id(), id, request.version(), false, request.note());
        return ApiResponse.ok(reads.adminDetail(id));
    }

    @PostMapping("/{id}/retain")
    public ApiResponse<?> retain(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody ActionRequest request) {
        service.adminDecision(admin.id(), id, request.version(), true, request.note());
        return ApiResponse.ok(reads.adminDetail(id));
    }
}
