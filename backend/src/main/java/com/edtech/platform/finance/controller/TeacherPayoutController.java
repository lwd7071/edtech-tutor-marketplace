package com.edtech.platform.finance.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.common.security.RateLimiterService;
import com.edtech.platform.finance.domain.PayoutStatus;
import com.edtech.platform.finance.dto.request.CreatePayoutRequest;
import com.edtech.platform.finance.dto.response.PayoutRequestView;
import com.edtech.platform.finance.service.PayoutService;
import com.edtech.platform.finance.idempotency.FinanceIdempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/payout-requests")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherPayoutController {

    private final PayoutService payoutService;
    private final RateLimiterService rateLimiterService;

    @PostMapping
    @FinanceIdempotent(operation = "TEACHER_PAYOUT_CREATE", responseType = PayoutRequestView.class)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PayoutRequestView> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreatePayoutRequest request
    ) {
        rateLimiterService.checkRateLimit("create_payout", user.id().toString(), 3, 3600);
        return ApiResponse.created(payoutService.createPayout(user.id(), request));
    }

    @GetMapping
    public ApiResponse<List<PayoutRequestView>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) PayoutStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        Page<PayoutRequestView> result = payoutService.findTeacherPayouts(user.id(), status, PageRequest.of(page, size));
        return ApiResponse.page(result.getContent(), PageMeta.from(result));
    }
}
