package com.edtech.platform.teacher.repository;

import com.edtech.platform.teacher.domain.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.edtech.platform.teacher.domain.ProfileStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {
    Optional<TeacherProfile> findByUserId(UUID userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT t.id FROM TeacherProfile t WHERE t.profileStatus = 'APPROVED'")
    java.util.List<UUID> findApprovedTeacherIds();
    
    Page<TeacherProfile> findByProfileStatus(ProfileStatus profileStatus, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TeacherProfile t where t.id = :id")
    Optional<TeacherProfile> findByIdForUpdate(@Param("id") UUID id);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t.id FROM TeacherProfile t " +
        "WHERE t.profileStatus = 'APPROVED' AND t.isVisible = true AND t.deleted = false " +
        "AND (:subjectId IS NULL OR EXISTS (SELECT 1 FROM TeacherSubject ts WHERE ts.teacher = t AND ts.subjectId = :subjectId AND ts.isActive = true AND ts.deleted = false)) " +
        "AND (:dayOfWeek IS NULL OR :startTime IS NULL OR :endTime IS NULL OR EXISTS (SELECT 1 FROM TeacherAvailability ta WHERE ta.teacher = t AND ta.dayOfWeek = :dayOfWeek AND ta.startTime <= :startTime AND ta.endTime >= :endTime AND ta.isActive = true AND ta.deleted = false))")
    java.util.Set<UUID> searchTeacherIds(@org.springframework.data.repository.query.Param("subjectId") UUID subjectId, 
                                         @org.springframework.data.repository.query.Param("dayOfWeek") java.time.DayOfWeek dayOfWeek, 
                                         @org.springframework.data.repository.query.Param("startTime") java.time.LocalTime startTime, 
                                         @org.springframework.data.repository.query.Param("endTime") java.time.LocalTime endTime);
}
