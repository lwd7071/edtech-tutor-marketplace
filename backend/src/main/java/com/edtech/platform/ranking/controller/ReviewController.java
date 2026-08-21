package com.edtech.platform.ranking.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.ranking.dto.request.CreateReviewRequest;
import com.edtech.platform.ranking.dto.response.ReviewView;
import com.edtech.platform.ranking.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/student/bookings/{id}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewView createReview(
            @PathVariable("id") UUID bookingId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return reviewService.createReview(authenticatedUser.id(), bookingId, request);
    }

    // @GetMapping("/api/public/teachers/{id}/reviews")
    // public Page<ReviewView> getPublicReviews(
    //         @PathVariable("id") UUID teacherId,
    //         Pageable pageable) {
    //     return reviewService.getPublicReviews(teacherId, pageable);
    // }
}
