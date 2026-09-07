package com.edtech.platform.booking.repository;
import com.edtech.platform.booking.domain.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import jakarta.persistence.LockModeType; import java.util.*;
public interface BookingRepository extends JpaRepository<Booking,UUID> {
 @Query("select b from Booking b where b.teacherId=:teacherId and (:status is null or b.status=:status) and (:from is null or b.startTime>=:from) and (:to is null or b.startTime<:to)") Page<Booking> findTeacher(UUID teacherId, BookingStatus status, java.time.Instant from, java.time.Instant to, Pageable pageable);
 @Query("select b from Booking b where b.studentId=:studentId and (:status is null or b.status=:status) and (:from is null or b.startTime>=:from) and (:to is null or b.startTime<:to)") Page<Booking> findStudent(UUID studentId, BookingStatus status, java.time.Instant from, java.time.Instant to, Pageable pageable);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select b from Booking b where b.id=:id") Optional<Booking> findByIdForUpdate(UUID id);
 @Query("select b from Booking b where b.status=com.edtech.platform.booking.domain.BookingStatus.SCHEDULED and b.endTime <= :cutoff") Page<Booking> findExpiryCandidates(java.time.Instant cutoff, Pageable pageable);
 @Query("select b from Booking b where b.status=com.edtech.platform.booking.domain.BookingStatus.SCHEDULED and b.endTime > :from and b.endTime <= :to") Page<Booking> findReminderCandidates(java.time.Instant from, java.time.Instant to, Pageable pageable);
    @Query("select count(b) > 0 from Booking b where b.teacherId = :teacherId and b.status = com.edtech.platform.booking.domain.BookingStatus.SCHEDULED and b.deleted = false and b.startTime < :endTime and b.endTime > :startTime")
    boolean existsOverlapTeacher(UUID teacherId, java.time.Instant startTime, java.time.Instant endTime);

    @Query("select count(b) > 0 from Booking b where b.studentId = :studentId and b.status = com.edtech.platform.booking.domain.BookingStatus.SCHEDULED and b.deleted = false and b.startTime < :endTime and b.endTime > :startTime")
    boolean existsOverlapStudent(UUID studentId, java.time.Instant startTime, java.time.Instant endTime);

    @Query("select count(b) from Booking b where b.teacherId = :teacherId and b.status = com.edtech.platform.booking.domain.BookingStatus.COMPLETED and b.deleted = false")
    int countCompletedSessionsByTeacherId(UUID teacherId);

    @Query("select count(b) from Booking b where b.teacherId = :teacherId and b.status in (com.edtech.platform.booking.domain.BookingStatus.COMPLETED, com.edtech.platform.booking.domain.BookingStatus.CANCELLED, com.edtech.platform.booking.domain.BookingStatus.SCHEDULED) and b.deleted = false")
    int countTotalSessionsByTeacherId(UUID teacherId);

    @Query("select count(b) from Booking b where b.teacherId = :teacherId and b.trial = true and b.status = com.edtech.platform.booking.domain.BookingStatus.COMPLETED and b.deleted = false")
    int countTrialCompletedSessionsByTeacherId(UUID teacherId);

    @Query("select count(b) > 0 from Booking b where b.teacherId = :teacherId and b.studentId = :studentId and b.status in :statuses and b.deleted = false")
    boolean existsByTeacherIdAndStudentIdAndStatusIn(UUID teacherId, UUID studentId, Collection<BookingStatus> statuses);

    @Query("select count(b) > 0 from Booking b where b.studentPackageId = :studentPackageId and b.status = :status and b.deleted = false")
    boolean existsByStudentPackageIdAndStatusAndDeletedFalse(@Param("studentPackageId") UUID studentPackageId, @Param("status") BookingStatus status);
}
