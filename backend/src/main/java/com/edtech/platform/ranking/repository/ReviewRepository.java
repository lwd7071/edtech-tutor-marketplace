package com.edtech.platform.ranking.repository;

import com.edtech.platform.ranking.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query(value = "SELECT r FROM Review r " +
            "JOIN com.edtech.platform.teacher.domain.TeacherProfile tp ON r.teacherId = tp.id " +
            "JOIN com.edtech.platform.auth.domain.User u ON tp.user.id = u.id " +
            "WHERE r.teacherId = :teacherId " +
            "AND r.isVisible = true " +
            "AND u.status = 'ACTIVE' " +
            "AND tp.profileStatus = 'APPROVED' " +
            "AND tp.isVisible = true")
    Page<Review> findPublicReviewsByTeacherId(@Param("teacherId") UUID teacherId, Pageable pageable);

    boolean existsByBookingId(UUID bookingId);
}
