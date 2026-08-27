package com.edtech.platform.ranking.repository;

import com.edtech.platform.ranking.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("SELECT r FROM Review r WHERE r.teacherId = :teacherId AND r.isVisible = true")
    Page<Review> findPublicReviewsByTeacherId(@Param("teacherId") UUID teacherId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.isVisible = true")
    Double findGlobalAverageRating();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.teacherId = :teacherId AND r.isVisible = true")
    int countVisibleReviewsByTeacherId(@Param("teacherId") UUID teacherId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.teacherId = :teacherId AND r.isVisible = true")
    Double findAverageRatingByTeacherId(@Param("teacherId") UUID teacherId);

    boolean existsByBookingId(UUID bookingId);
}
