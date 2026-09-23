package com.edtech.platform.auth.repository;

import com.edtech.platform.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    @org.springframework.data.jpa.repository.Query("SELECT u.id FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND u.status = 'ACTIVE' AND u.deleted = false")
    java.util.Set<UUID> searchIdsByKeyword(@org.springframework.data.repository.query.Param("keyword") String keyword);

    @Query(value = "select u from User u where u.role in :roles and u.status in :statuses " +
            "and (:role is null or u.role = :role) and (:status is null or u.status = :status) " +
            "and (:keyword is null or lower(u.fullName) like lower(concat('%', :keyword, '%')) " +
            "or lower(u.email) like lower(concat('%', :keyword, '%')))",
            countQuery = "select count(u) from User u where u.role in :roles and u.status in :statuses " +
                    "and (:role is null or u.role = :role) and (:status is null or u.status = :status) " +
                    "and (:keyword is null or lower(u.fullName) like lower(concat('%', :keyword, '%')) " +
                    "or lower(u.email) like lower(concat('%', :keyword, '%')))")
    org.springframework.data.domain.Page<User> findDirectory(
            @Param("roles") java.util.Collection<com.edtech.platform.auth.domain.Role> roles,
            @Param("statuses") java.util.Collection<com.edtech.platform.auth.domain.UserStatus> statuses,
            @Param("role") com.edtech.platform.auth.domain.Role role,
            @Param("status") com.edtech.platform.auth.domain.UserStatus status,
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);
}
