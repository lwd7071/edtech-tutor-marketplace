package com.edtech.platform.finance.repository;

import com.edtech.platform.finance.domain.TeacherBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherBankAccountRepository extends JpaRepository<TeacherBankAccount, UUID> {

    List<TeacherBankAccount> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);

    Optional<TeacherBankAccount> findByIdAndTeacherId(UUID id, UUID teacherId);

    Optional<TeacherBankAccount> findByTeacherIdAndIsDefaultTrue(UUID teacherId);

    boolean existsByTeacherId(UUID teacherId);

    @Modifying
    @Query("UPDATE TeacherBankAccount b SET b.isDefault = false WHERE b.teacherId = :teacherId AND b.id <> :excludeId")
    void unsetDefaultExcept(@Param("teacherId") UUID teacherId, @Param("excludeId") UUID excludeId);
}