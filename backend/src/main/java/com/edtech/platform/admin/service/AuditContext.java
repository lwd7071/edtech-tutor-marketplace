package com.edtech.platform.admin.service;

public record AuditContext(String ipAddress, String userAgent) {
    public AuditContext {
        ipAddress = truncate(ipAddress, 64);
        userAgent = truncate(userAgent, 2000);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
