package com.edtech.platform.booking.controller;

import com.edtech.platform.booking.dto.response.SessionReportView;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/session-reports")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentSessionReportController {

    private final BookingReadService bookingReads;

    @GetMapping
    public ApiResponse<List<SessionReportView>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        Page<SessionReportView> result = bookingReads.findStudentSessionReports(
                user.id(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"))
        );
        return ApiResponse.page(result.getContent(), PageMeta.from(result));
    }
}
