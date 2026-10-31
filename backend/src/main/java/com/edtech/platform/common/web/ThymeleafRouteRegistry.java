package com.edtech.platform.common.web;

import java.util.List;

/** Route inventory for parity checks; Spring Security remains the authorization source. */
public final class ThymeleafRouteRegistry {
    private static final List<Route> ROUTES = List.of(
            route("/", Role.PUBLIC),
            route("/teachers", Role.PUBLIC),
            route("/teachers/{id}", Role.PUBLIC),
            route("/subjects", Role.PUBLIC),
            route("/ranking", Role.PUBLIC),
            route("/how-it-works", Role.PUBLIC),
            route("/become-a-tutor", Role.PUBLIC),
            route("/terms", Role.PUBLIC),
            route("/privacy", Role.PUBLIC),
            route("/support", Role.PUBLIC),

            route("/auth/login", Role.GUEST),
            route("/auth/register", Role.GUEST),
            route("/auth/verify-email", Role.GUEST),
            route("/auth/forgot-password", Role.GUEST),
            route("/auth/reset-password", Role.GUEST),
            route("/auth/oauth/role", Role.AUTH_FLOW),
            new Route("/oauth2/callback", Role.AUTH_FLOW, State.COMPATIBILITY_REDIRECT, true),
            route("/forbidden", Role.ANY),

            route("/student", Role.STUDENT),
            route("/student/packages", Role.STUDENT),
            route("/student/packages/{id}", Role.STUDENT),
            route("/student/checkout/{invoiceId}", Role.STUDENT),
            route("/student/payment-result/{invoiceId}", Role.STUDENT),
            route("/student/payments/callback", Role.STUDENT),
            route("/student/bookings", Role.STUDENT),
            route("/student/bookings/{id}", Role.STUDENT),
            route("/student/assignments", Role.STUDENT),
            route("/student/assignments/{id}", Role.STUDENT),
            route("/student/session-reports", Role.STUDENT),
            route("/student/messages", Role.STUDENT),
            route("/student/notifications", Role.STUDENT),
            route("/student/requests", Role.STUDENT),
            route("/student/profile", Role.STUDENT),

            route("/teacher", Role.TEACHER),
            route("/teacher/profile", Role.TEACHER),
            route("/teacher/documents", Role.TEACHER),
            route("/teacher/credentials", Role.TEACHER),
            route("/teacher/subjects", Role.TEACHER),
            route("/teacher/subject-proposals", Role.TEACHER),
            route("/teacher/packages", Role.TEACHER),
            route("/teacher/packages/create", Role.TEACHER),
            route("/teacher/packages/{id}", Role.TEACHER),
            route("/teacher/availability", Role.TEACHER),
            route("/teacher/students", Role.TEACHER),
            route("/teacher/students/{studentId}", Role.TEACHER),
            route("/teacher/bookings", Role.TEACHER),
            route("/teacher/bookings/{id}", Role.TEACHER),
            route("/teacher/trial-requests", Role.TEACHER),
            route("/teacher/assignments", Role.TEACHER),
            route("/teacher/assignments/new", Role.TEACHER),
            route("/teacher/assignments/{id}", Role.TEACHER),
            route("/teacher/submissions/{id}", Role.TEACHER),
            route("/teacher/messages", Role.TEACHER),
            route("/teacher/notifications", Role.TEACHER),
            route("/teacher/stats", Role.TEACHER),
            route("/teacher/wallet", Role.TEACHER),
            route("/teacher/bank-accounts", Role.TEACHER),
            route("/teacher/payouts", Role.TEACHER),

            route("/admin", Role.ADMIN),
            route("/admin/dashboard", Role.ADMIN),
            route("/admin/teachers", Role.ADMIN),
            route("/admin/credentials", Role.ADMIN),
            route("/admin/subjects", Role.ADMIN),
            route("/admin/users", Role.ADMIN),
            route("/admin/refunds", Role.ADMIN),
            route("/admin/extensions", Role.ADMIN),
            route("/admin/payouts", Role.ADMIN),
            route("/admin/booking-settlements", Role.ADMIN),
            route("/admin/settings", Role.ADMIN),
            route("/admin/audit-logs", Role.ADMIN),

            new Route("/teacher/booking-requests", Role.TEACHER, State.LIVE, false),
            new Route("/sandbox/components", Role.INTERNAL, State.DEVELOPMENT_ONLY, false)
    );

    private ThymeleafRouteRegistry() {}

    public static List<Route> routes() {
        return ROUTES;
    }

    private static Route route(String pathPattern, Role role) {
        return new Route(pathPattern, role, State.LIVE, true);
    }

    public record Route(String pathPattern, Role expectedRole, State state, boolean legacyParityRoute) {}

    public enum Role { PUBLIC, GUEST, AUTH_FLOW, STUDENT, TEACHER, ADMIN, ANY, INTERNAL }

    public enum State { LIVE, PLACEHOLDER, COMPATIBILITY_REDIRECT, DEVELOPMENT_ONLY }
}
