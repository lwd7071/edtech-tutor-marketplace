package com.edtech.platform.enrollment.dto;
import com.edtech.platform.enrollment.domain.StudentPackage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record StudentPackageDetail(UUID id, UUID pricingPackageId, UUID invoiceId, String packageName,
 int totalSessions, int remainingSessions, long purchasePriceVnd, BigDecimal commissionRate,
 Instant startsAt, Instant expiresAt, String status) {
 public static StudentPackageDetail from(StudentPackage p) { return new StudentPackageDetail(p.getId(),p.getPricingPackageId(),p.getInvoiceId(),p.getPackageNameSnapshot(),p.getTotalSessions(),p.getRemainingSessions(),p.getPurchasePriceVnd(),p.getCommissionRate(),p.getStartsAt(),p.getExpiresAt(),p.getStatus().name()); }
}
