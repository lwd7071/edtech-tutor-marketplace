package com.edtech.platform.communication.service;

import java.util.UUID;

final class NotificationRoutes {
    private NotificationRoutes() {}
    static String resolve(String role, String type, UUID id) {
        if (role == null || type == null || id == null) return null;
        String base = switch (role) { case "STUDENT" -> "/student"; case "TEACHER" -> "/teacher"; default -> null; };
        if (base == null) return null;
        return switch (type) {
            case "BOOKING" -> base + "/bookings/" + id;
            case "ASSIGNMENT" -> base + "/assignments/" + id;
            case "MESSAGE", "CONVERSATION" -> base + "/messages";
            case "STUDENT_PACKAGE" -> "STUDENT".equals(role) ? base + "/packages/" + id : base + "/students";
            case "INVOICE" -> "STUDENT".equals(role) ? base + "/payment-result/" + id : base + "/wallet";
            case "REFUND", "EXTENSION" -> "STUDENT".equals(role) ? base + "/requests" : null;
            case "PAYOUT" -> "TEACHER".equals(role) ? base + "/payouts" : null;
            default -> null;
        };
    }
}
