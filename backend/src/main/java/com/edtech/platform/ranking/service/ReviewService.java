package com.edtech.platform.ranking.service;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.ranking.domain.Review;
import com.edtech.platform.ranking.dto.request.CreateReviewRequest;
import com.edtech.platform.ranking.dto.response.ReviewView;
import com.edtech.platform.ranking.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import org.springframework.context.ApplicationEventPublisher;
import com.edtech.platform.ranking.domain.event.ReviewCreatedEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReviewView createReview(UUID studentId, UUID bookingId, CreateReviewRequest request) {
        if (reviewRepository.existsByBookingId(bookingId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS");
        }

        String sql = "SELECT status, student_id, teacher_id FROM bookings WHERE id = ? AND is_deleted = false";
        List<Map<String, Object>> bookings = jdbcTemplate.queryForList(sql, bookingId);

        if (bookings.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "REVIEW_NOT_ALLOWED");
        }

        Map<String, Object> booking = bookings.get(0);
        String status = (String) booking.get("status");
        UUID bookingStudentId = (UUID) booking.get("student_id");
        UUID teacherId = (UUID) booking.get("teacher_id");

        if (!"COMPLETED".equals(status) || !studentId.equals(bookingStudentId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "REVIEW_NOT_ALLOWED");
        }

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

    private ReviewView mapToView(Review review) {
        User student = userRepository.findById(review.getStudentId()).orElse(null);
        
        ReviewView.StudentDto studentDto = null;
        if (student != null) {
            studentDto = ReviewView.StudentDto.builder()
                    .id(student.getId())
                    .fullName(student.getFullName())
                    .avatarUrl(student.getAvatarUrl())
                    .build();
        }

        return ReviewView.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .student(studentDto)
                .build();
    }
}
