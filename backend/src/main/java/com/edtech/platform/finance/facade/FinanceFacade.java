package com.edtech.platform.finance.facade;

import java.util.UUID;

public interface FinanceFacade {
    void creditTeacherPendingBalance(UUID teacherId, long netAmount, UUID invoiceId, String invoiceNumber);
    void settleBookingSession(UUID teacherId, UUID bookingId, long amount);
}
