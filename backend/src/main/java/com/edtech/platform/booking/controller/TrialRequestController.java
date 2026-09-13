package com.edtech.platform.booking.controller;

import com.edtech.platform.booking.domain.TrialRequestStatus;
import com.edtech.platform.booking.dto.request.AcceptTrialRequest;
import com.edtech.platform.booking.dto.request.CreateTrialRequest;
import com.edtech.platform.booking.dto.request.RejectTrialRequest;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.dto.response.TrialRequestView;
import com.edtech.platform.booking.service.TrialRequestService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TrialRequestController {

    private final TrialRequestService service;

    @PostMapping("/api/student/trials/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole("STUDENT")
    public ApiResponse<TrialRequestView> create(@AuthenticationPrincipal AuthenticatedUser u, @Valid @RequestBody CreateTrialRequest r) {
        return ApiResponse.created(service.create(u.id(), r));
    }

    @GetMapping("/api/student/trial-requests")
    @RequireRole("STUDENT")
    public ApiResponse<List<TrialRequestView>> listForStudent(
            @AuthenticationPrincipal AuthenticatedUser u,
            @RequestParam(required = false) TrialRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TrialRequestView> p = service.findForStudent(u.id(), status,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.page(p.getContent(), PageMeta.from(p));
    }

    @GetMapping("/api/teacher/trial-requests")
    @RequireRole("TEACHER")
    public ApiResponse<List<TrialRequestView>> list(
            @AuthenticationPrincipal AuthenticatedUser u,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TrialRequestView> p = service.find(u.id(), PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.page(p.getContent(), PageMeta.from(p));
    }

    @PostMapping("/api/teacher/trial-requests/{id}/accept")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole("TEACHER")
    public ApiResponse<BookingDetail> accept(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable UUID id, @Valid @RequestBody AcceptTrialRequest r) {
        return ApiResponse.created(service.accept(u.id(), id, r));
    }

    @PostMapping("/api/teacher/trial-requests/{id}/reject")
    @RequireRole("TEACHER")
    public ApiResponse<TrialRequestView> reject(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable UUID id, @Valid @RequestBody RejectTrialRequest r) {
        return ApiResponse.ok(service.reject(u.id(), id, r));
    }
}
