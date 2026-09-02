package com.edtech.platform.enrollment.domain;

public enum StudentPackageStatus {
    /** Legacy schema compatibility only. New packages are created after successful payment as ACTIVE. */
    PENDING_PAYMENT,
    ACTIVE,
    COMPLETED,
    REFUND_PENDING,
    REFUNDED,
    LOCKED_EXPIRED
}
