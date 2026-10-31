package com.edtech.platform.ranking.facade.impl;

import com.edtech.platform.ranking.dto.response.ReviewView;
import com.edtech.platform.ranking.facade.PublicReviewFacade;
import com.edtech.platform.ranking.facade.dto.PublicReviewSnapshot;
import com.edtech.platform.ranking.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PublicReviewFacadeImpl implements PublicReviewFacade {
    private final ReviewService reviewService;

    @Override
    public Page<PublicReviewSnapshot> getPublicReviews(UUID teacherId, Pageable pageable) {
        return reviewService.getPublicReviews(teacherId, pageable).map(PublicReviewFacadeImpl::snapshot);
    }

    private static PublicReviewSnapshot snapshot(ReviewView review) {
        ReviewView.StudentDto student = review.getStudent();
        return new PublicReviewSnapshot(
                review.getId(),
                review.getRating() == null ? 0 : review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                student == null ? null : student.getFullName(),
                student == null ? null : student.getAvatarUrl());
    }
}
