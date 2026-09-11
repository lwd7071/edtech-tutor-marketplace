package com.edtech.platform.enrollment.facade.impl;

import com.edtech.platform.booking.facade.EnrollmentBookingFacade;
import com.edtech.platform.booking.facade.dto.BookingPackageSnapshot;
import com.edtech.platform.enrollment.domain.StudentPackage;
import com.edtech.platform.enrollment.repository.StudentPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentBookingFacadeImpl implements EnrollmentBookingFacade {

    private final StudentPackageRepository repo;
    private final Clock clock;

    private StudentPackage get(UUID id) {
        return repo.findByIdForUpdate(id).orElseThrow();
    }

    private BookingPackageSnapshot snap(StudentPackage p) {
        return new BookingPackageSnapshot(
                p.getId(),
                p.getStudentId(),
                p.getTeacherId(),
                p.getSubjectId(),
                p.getStatus().name(),
                p.getRemainingSessions(),
                p.getReservedSessions(),
                p.getCompletedSessions(),
                p.getRefundedSessions(),
                p.getTotalSessions(),
                p.getPurchasePriceVnd(),
                p.getCommissionRate(),
                p.getExpiresAt(),
                p.getVersion()
        );
    }

    @Override
    @Transactional
    public BookingPackageSnapshot inspect(UUID id, UUID student) {
        StudentPackage p = get(id);
        if (student != null && !p.getStudentId().equals(student)) {
            throw new IllegalArgumentException("ownership mismatch");
        }
        return snap(p);
    }

    @Override
    @Transactional
    public void reserveSession(UUID id) {
        get(id).reserveSession();
    }

    @Override
    @Transactional
    public void completeReservedSession(UUID id) {
        get(id).completeReservedSession();
    }

    @Override
    @Transactional
    public void releaseReservedSession(UUID id) {
        get(id).releaseReservedSession();
    }

    @Override
    @Transactional
    public void lockExpiredPackages(Instant cutoff, Pageable pageable) {
        repo.findExpired(cutoff, pageable).forEach(candidate -> {
            repo.findByIdForUpdate(candidate.getId()).ifPresent(pkg -> pkg.lockExpired(clock.instant()));
        });
    }
}
