package com.edtech.platform.booking.facade.dto;
import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
public record BookingPackageSnapshot(UUID packageId,UUID studentId,UUID teacherId,UUID subjectId,String status,int remainingSessions,int reservedSessions,int completedSessions,int refundedSessions,int totalSessions,long purchasePriceVnd,BigDecimal commissionRate,Instant expiresAt,long version) {}
