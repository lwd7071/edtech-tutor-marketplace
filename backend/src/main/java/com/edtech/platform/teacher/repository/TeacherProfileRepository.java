package com.edtech.platform.teacher.repository;

import com.edtech.platform.teacher.domain.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {
    Optional<TeacherProfile> findByUserId(UUID userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT t.id FROM TeacherProfile t WHERE t.profileStatus = 'APPROVED'")
    java.util.List<UUID> findApprovedTeacherIds();
}
