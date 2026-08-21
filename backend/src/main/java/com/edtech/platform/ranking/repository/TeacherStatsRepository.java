package com.edtech.platform.ranking.repository;

import com.edtech.platform.ranking.domain.TeacherStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TeacherStatsRepository extends JpaRepository<TeacherStats, UUID> {

    @Query(value = "SELECT ts.* FROM teacher_stats ts " +
            "JOIN teacher_profiles tp ON ts.teacher_id = tp.id " +
            "JOIN users u ON tp.user_id = u.id " +
            "WHERE u.status = 'ACTIVE' " +
            "AND tp.profile_status = 'APPROVED' " +
            "AND tp.is_visible = true " +
            "AND tp.is_deleted = false " +
            "AND u.is_deleted = false " +
            "AND (:subjectId IS NULL OR EXISTS (SELECT 1 FROM teacher_subjects subj WHERE subj.teacher_id = ts.teacher_id AND subj.subject_id = CAST(CAST(:subjectId AS text) AS uuid) AND subj.is_active = true AND subj.is_deleted = false)) " +
            "ORDER BY ts.global_rank ASC",
            countQuery = "SELECT count(*) FROM teacher_stats ts " +
            "JOIN teacher_profiles tp ON ts.teacher_id = tp.id " +
            "JOIN users u ON tp.user_id = u.id " +
            "WHERE u.status = 'ACTIVE' " +
            "AND tp.profile_status = 'APPROVED' " +
            "AND tp.is_visible = true " +
            "AND tp.is_deleted = false " +
            "AND u.is_deleted = false " +
            "AND (:subjectId IS NULL OR EXISTS (SELECT 1 FROM teacher_subjects subj WHERE subj.teacher_id = ts.teacher_id AND subj.subject_id = CAST(CAST(:subjectId AS text) AS uuid) AND subj.is_active = true AND subj.is_deleted = false))",
            nativeQuery = true)
    Page<TeacherStats> findGlobalRanking(@Param("subjectId") String subjectId, Pageable pageable);
}
