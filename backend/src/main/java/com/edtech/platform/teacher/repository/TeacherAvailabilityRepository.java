package com.edtech.platform.teacher.repository;

import com.edtech.platform.teacher.domain.TeacherAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailability, UUID> {
    List<TeacherAvailability> findByTeacherId(UUID teacherId);

    @Modifying
    @Query("DELETE FROM TeacherAvailability ta WHERE ta.teacher.id = :teacherId")
    void deleteByTeacherId(UUID teacherId);
}
