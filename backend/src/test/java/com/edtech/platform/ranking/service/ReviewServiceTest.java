package com.edtech.platform.ranking.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.booking.facade.BookingEligibilityFacade;
import com.edtech.platform.ranking.domain.Review;
import com.edtech.platform.ranking.dto.request.CreateReviewRequest;
import com.edtech.platform.ranking.dto.response.ReviewView;
import com.edtech.platform.ranking.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private IdentityFacade identityFacade;
    @Mock private BookingEligibilityFacade bookingEligibilityFacade;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReviewService reviewService;

    private UUID studentId;
    private UUID teacherId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    // ── Slice 1: duplicate booking → CONFLICT ────────────────────────────────

    @Test
    void createReview_throwsConflict_whenBookingAlreadyReviewed() {
        when(reviewRepository.existsByBookingId(bookingId)).thenReturn(true);

        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating((short) 5);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.createReview(studentId, bookingId, request));

        assertEquals(409, ex.getStatusCode().value());
    }

    // ── Slice 2: booking not eligible → UNPROCESSABLE ────────────────────────

    @Test
    void createReview_throwsUnprocessable_whenBookingNotEligible() {
        when(reviewRepository.existsByBookingId(bookingId)).thenReturn(false);
        when(bookingEligibilityFacade.getTeacherIdForReviewableBooking(studentId, bookingId))
                .thenReturn(Optional.empty());

        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating((short) 4);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.createReview(studentId, bookingId, request));

        assertEquals(422, ex.getStatusCode().value());
    }

    // ── Slice 3: happy path — review is saved and event published ────────────

    @Test
    void createReview_savesReviewAndPublishesEvent_whenEligible() {
        when(reviewRepository.existsByBookingId(bookingId)).thenReturn(false);
        when(bookingEligibilityFacade.getTeacherIdForReviewableBooking(studentId, bookingId))
                .thenReturn(Optional.of(teacherId));

        Review savedReview = Review.builder()
                .bookingId(bookingId)
                .studentId(studentId)
                .teacherId(teacherId)
                .rating((short) 5)
                .comment("Excellent teacher!")
                .build();
        ReflectionTestUtils.setField(savedReview, "id", UUID.randomUUID());

        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(identityFacade.getIdentity(studentId)).thenReturn(Optional.empty());

        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating((short) 5);
        request.setComment("Excellent teacher!");

        ReviewView result = reviewService.createReview(studentId, bookingId, request);

        assertNotNull(result);
        assertEquals((short) 5, result.getRating());
        verify(reviewRepository).save(any(Review.class));
        verify(eventPublisher).publishEvent(any(Object.class));
    }
}
