package com.edtech.platform.ranking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.booking.facade.BookingEligibilityFacade;
import com.edtech.platform.ranking.domain.Review;
import com.edtech.platform.ranking.dto.request.CreateReviewRequest;
import com.edtech.platform.ranking.dto.response.ReviewView;
import com.edtech.platform.ranking.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;

import org.springframework.context.ApplicationEventPublisher;
import com.edtech.platform.ranking.domain.event.ReviewCreatedEvent;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final IdentityFacade identityFacade;
    private final BookingEligibilityFacade bookingEligibilityFacade;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReviewView createReview(UUID studentId, UUID bookingId, CreateReviewRequest request) {
        if (reviewRepository.existsByBookingId(bookingId)) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        var teacherIdOpt = bookingEligibilityFacade.getTeacherIdForReviewableBooking(studentId, bookingId);
        if (teacherIdOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
        UUID teacherId = teacherIdOpt.get();

        Review review = Review.builder()
                .bookingId(bookingId)
                .studentId(studentId)
                .teacherId(teacherId)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);
        
        eventPublisher.publishEvent(new ReviewCreatedEvent(
                savedReview.getId(),
                teacherId,
                studentId,
                request.getRating() != null ? request.getRating().intValue() : 0
        ));
        
        return mapToView(savedReview);
    }

    @Transactional(readOnly = true)
    public Page<ReviewView> getPublicReviews(UUID teacherId, Pageable pageable) {
        return reviewRepository.findPublicReviewsByTeacherId(teacherId, pageable)
                .map(this::mapToView);
    }

    @Transactional(readOnly = true)
    public ReviewView getStudentBookingReview(UUID studentId, UUID bookingId) {
        return reviewRepository.findByBookingIdAndStudentId(bookingId, studentId)
                .map(this::mapToView)
                .orElse(null);
    }

    private ReviewView mapToView(Review review) {
        var studentSnapshotOpt = identityFacade.getIdentity(review.getStudentId());
        
        ReviewView.StudentDto studentDto = null;
        if (studentSnapshotOpt.isPresent()) {
            var studentSnapshot = studentSnapshotOpt.get();
            studentDto = ReviewView.StudentDto.builder()
                    .id(studentSnapshot.id())
                    .fullName(studentSnapshot.fullName())
                    .avatarUrl(studentSnapshot.avatarUrl())
                    .build();
        }

        return ReviewView.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().atZone(java.time.ZoneId.of("UTC")) : null)
                .student(studentDto)
                .build();
    }
}
