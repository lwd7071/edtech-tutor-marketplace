package com.edtech.platform.finance.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.finance.dto.request.CreateRefundRequest;
import com.edtech.platform.finance.dto.response.RefundRequestView;
import com.edtech.platform.finance.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/refund-requests")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentRefundController {

    private final RefundService refundService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RefundRequestView> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateRefundRequest request
    ) {
        return ApiResponse.created(refundService.createRefund(user.id(), request));
    }

    @GetMapping
    public ApiResponse<List<RefundRequestView>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        Page<RefundRequestView> result = refundService.findStudentRefunds(user.id(), PageRequest.of(page, size));
        return ApiResponse.page(result.getContent(), PageMeta.from(result));
    }
}