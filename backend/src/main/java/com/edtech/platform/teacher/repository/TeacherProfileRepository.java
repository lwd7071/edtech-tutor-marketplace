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
}
