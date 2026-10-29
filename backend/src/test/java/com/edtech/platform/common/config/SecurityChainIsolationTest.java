package com.edtech.platform.common.config;

import com.edtech.platform.catalog.web.ForbiddenPageController;
import com.edtech.platform.catalog.web.ServiceUnavailablePageController;
import com.edtech.platform.admin.dto.response.AdminDashboardView;
import com.edtech.platform.admin.service.AdminDashboardService;
import com.edtech.platform.admin.web.AdminDashboardPageController;
import com.edtech.platform.admin.web.AdminTeacherApprovalsPageController;
import com.edtech.platform.admin.web.AdminCredentialsPageController;
import com.edtech.platform.admin.web.AdminSubjectsPageController;
import com.edtech.platform.admin.web.AdminUsersPageController;
import com.edtech.platform.admin.web.AdminSettingsPageController;
import com.edtech.platform.admin.web.AdminAuditLogsPageController;
import com.edtech.platform.finance.web.AdminRefundsPageController;
import com.edtech.platform.finance.web.AdminExtensionsPageController;
import com.edtech.platform.finance.web.AdminPayoutsPageController;
import com.edtech.platform.booking.web.AdminBookingSettlementsPageController;
import com.edtech.platform.admin.service.AdminApprovalService;
import com.edtech.platform.admin.service.AdminUserDirectoryService;
import com.edtech.platform.admin.service.AdminSettingsService;
import com.edtech.platform.admin.service.AuditLogQueryService;
import com.edtech.platform.catalog.web.TeacherPackagesPageController;
import com.edtech.platform.teacher.web.TeacherAvailabilityPageController;
import com.edtech.platform.teacher.service.TeacherAvailabilityService;
import com.edtech.platform.enrollment.web.TeacherStudentsPageController;
import com.edtech.platform.enrollment.service.TeacherLearnerService;
import com.edtech.platform.catalog.service.PricingPackageService;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.communication.web.NotificationPageController;
import com.edtech.platform.communication.service.NotificationService;
import com.edtech.platform.communication.web.StudentMessagesPageController;
import com.edtech.platform.communication.web.TeacherMessagesPageController;
import com.edtech.platform.ranking.web.TeacherStatsPageController;
import com.edtech.platform.ranking.service.TeacherStatsService;
import com.edtech.platform.teacher.web.TeacherProfilePageController;
import com.edtech.platform.teacher.service.TeacherProfileService;
import com.edtech.platform.catalog.service.AdministrativeLocationService;
import com.edtech.platform.teacher.web.TeacherDocumentsPageController;
import com.edtech.platform.teacher.service.TeacherDocumentService;
import com.edtech.platform.teacher.web.TeacherCredentialsPageController;
import com.edtech.platform.teacher.service.TeacherCredentialService;
import com.edtech.platform.teacher.web.TeacherSubjectsPageController;
import com.edtech.platform.teacher.service.TeacherSubjectService;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.web.TeacherSubjectProposalsPageController;
import com.edtech.platform.subject.service.SubjectProposalService;
import com.edtech.platform.communication.service.ChatCommandService;
import com.edtech.platform.communication.service.ChatQueryService;
import com.edtech.platform.communication.service.ConversationService;
import com.edtech.platform.common.service.AttachmentService;
import com.edtech.platform.finance.web.StudentRequestsPageController;
import com.edtech.platform.finance.web.TeacherFinancePageController;
import com.edtech.platform.dashboard.web.TeacherDashboardPageController;
import com.edtech.platform.auth.web.StudentProfilePageController;
import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.service.ParentContactService;
import com.edtech.platform.booking.web.StudentBookingPageController;
import com.edtech.platform.booking.web.TeacherBookingPageController;
import com.edtech.platform.booking.web.TeacherTrialRequestsPageController;
import com.edtech.platform.learning.web.TeacherAssignmentPageController;
import com.edtech.platform.learning.service.AssignmentReadService;
import com.edtech.platform.learning.service.TeacherAssignmentService;
import com.edtech.platform.common.facade.AttachmentFacade;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.booking.service.BookingRequestService;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.finance.service.RefundService;
import com.edtech.platform.finance.service.ExtensionService;
import com.edtech.platform.booking.service.TrialRequestService;
import com.edtech.platform.enrollment.service.StudentPackageReadService;
import com.edtech.platform.finance.service.FinanceCommandExecutor;
import com.edtech.platform.finance.service.FinanceProofStorage;
import com.edtech.platform.finance.service.BankAccountService;
import com.edtech.platform.finance.service.PayoutService;
import com.edtech.platform.finance.service.WalletQueryService;
import com.edtech.platform.common.security.RateLimiterService;
import com.edtech.platform.auth.security.OAuth2AuthenticationFailureHandler;
import com.edtech.platform.auth.security.OAuth2AuthenticationSuccessHandler;
import com.edtech.platform.common.config.properties.CorsProperties;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtAuthenticationFilter;
import com.edtech.platform.common.security.HtmlSessionAuthenticationFilter;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.RestAccessDeniedHandler;
import com.edtech.platform.common.security.RestAuthenticationEntryPoint;
import com.edtech.platform.common.security.UserStatusCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import com.edtech.platform.auth.service.SessionService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.time.Clock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest({ForbiddenPageController.class, ServiceUnavailablePageController.class, AdminDashboardPageController.class, AdminTeacherApprovalsPageController.class, AdminCredentialsPageController.class, AdminSubjectsPageController.class, AdminUsersPageController.class, AdminSettingsPageController.class, AdminAuditLogsPageController.class, AdminRefundsPageController.class, AdminExtensionsPageController.class, AdminPayoutsPageController.class, AdminBookingSettlementsPageController.class, NotificationPageController.class, StudentRequestsPageController.class,
        StudentProfilePageController.class, StudentBookingPageController.class, StudentMessagesPageController.class,
        TeacherMessagesPageController.class, TeacherStatsPageController.class, TeacherProfilePageController.class,
        TeacherDocumentsPageController.class, TeacherCredentialsPageController.class, TeacherSubjectsPageController.class,
        TeacherSubjectProposalsPageController.class, TeacherPackagesPageController.class, TeacherAvailabilityPageController.class,
        TeacherStudentsPageController.class, TeacherBookingPageController.class, TeacherTrialRequestsPageController.class,
        TeacherAssignmentPageController.class, TeacherFinancePageController.class, TeacherDashboardPageController.class})
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        HtmlSessionAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class SecurityChainIsolationTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockBean private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    @MockBean private CorsProperties corsProperties;
    @MockBean private NotificationService notifications;
    @MockBean private RefundService refunds;
    @MockBean private ExtensionService extensions;
    @MockBean private TrialRequestService trials;
    @MockBean private StudentPackageReadService packages;
    @MockBean private FinanceCommandExecutor financeCommands;
    @MockBean private FinanceProofStorage financeProofStorage;
    @MockBean private CredentialProofStorage credentialProofStorage;
    @MockBean private BankAccountService bankAccounts;
    @MockBean private PayoutService payouts;
    @MockBean private RateLimiterService rateLimiter;
    @MockBean private WalletQueryService wallets;
    @MockBean private ParentContactService parentContacts;
    @MockBean private IdentityFacade identities;
    @MockBean private BookingRequestService bookingRequests;
    @MockBean private BookingReadService bookingReads;
    @MockBean private BookingService bookingCommands;
    @MockBean private ChatQueryService chatQueries;
    @MockBean private ChatCommandService chatCommands;
    @MockBean private ConversationService conversations;
    @MockBean private AttachmentService attachments;
    @MockBean private TeacherStatsService teacherStats;
    @MockBean private TeacherProfileService teacherProfiles;
    @MockBean private AdministrativeLocationService locations;
    @MockBean private TeacherDocumentService teacherDocuments;
    @MockBean private TeacherCredentialService teacherCredentials;
    @MockBean private TeacherSubjectService teacherSubjects;
    @MockBean private SubjectFacade subjects;
    @MockBean private SubjectProposalService subjectProposals;
    @MockBean private PricingPackageService pricingPackages;
    @MockBean private EnrollmentFacade enrollmentFacade;
    @MockBean private TeacherFacade teacherFacade;
    @MockBean private TeacherAvailabilityService teacherAvailability;
    @MockBean private TeacherLearnerService teacherLearners;
    @MockBean private AssignmentReadService assignmentReads;
    @MockBean private TeacherAssignmentService teacherAssignments;
    @MockBean private AttachmentFacade attachmentFacade;
    @MockBean private AdminDashboardService adminDashboardService;
    @MockBean private AdminApprovalService adminApprovalService;
    @MockBean private AdminUserDirectoryService adminUserDirectoryService;
    @MockBean private AdminSettingsService adminSettingsService;
    @MockBean private AuditLogQueryService auditLogQueryService;

    @Test
    void apiDoesNotAcceptAnAdminHttpSessionAsAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/session-isolation")
                        .session(sessionWithRole("ADMIN")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminDashboardRequiresAdminSession() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/admin").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.when(adminDashboardService.getDashboard()).thenReturn(
                new AdminDashboardView(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        mockMvc.perform(get("/admin/dashboard").session(sessionWithRole("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void sessionStatusLookupFailureFailsClosedWithRendered503AndPreservesSession() throws Exception {
        UUID userId = UUID.randomUUID();
        var user = new AuthenticatedUser(userId, "admin@example.test", "ADMIN");
        org.mockito.Mockito.when(clock.instant()).thenReturn(java.time.Instant.parse("2026-10-28T12:00:00Z"));
        org.mockito.Mockito.when(jwtTokenProvider.parseAccessToken("session-access"))
                .thenReturn(new JwtTokenProvider.VerifiedAccessToken(user,
                        java.time.Instant.parse("2026-10-28T12:15:00Z")));
        org.mockito.Mockito.when(userStatusCacheService.resolve(userId))
                .thenThrow(new org.springframework.transaction.CannotCreateTransactionException("database unavailable"));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE,
                new com.edtech.platform.common.security.HtmlSessionState("session-access", "private-refresh",
                        java.time.Instant.parse("2026-10-28T11:59:00Z")));

        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isServiceUnavailable())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl(
                        "/service-unavailable"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Cache-Control", "no-store, max-age=0"));
        mockMvc.perform(get("/service-unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa thể tải trang này")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thử tải lại")));
        org.junit.jupiter.api.Assertions.assertNotNull(
                session.getAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE));
    }

    @Test
    void adminTeacherApprovalRoutesRequireAdminSessionAndCsrfForDecisions() throws Exception {
        mockMvc.perform(get("/admin/teachers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/admin/teachers").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/teachers").session(sessionWithRole("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mockMvc.perform(post("/admin/teachers/" + UUID.randomUUID() + "/approve")
                        .session(sessionWithRole("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/teachers/" + UUID.randomUUID() + "/approve")
                        .session(sessionWithRole("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminCredentialsRequireAdminSessionAndCsrfForDecisions() throws Exception {
        mockMvc.perform(get("/admin/credentials"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/admin/credentials").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.when(teacherCredentials.adminList("PENDING")).thenReturn(List.of());
        mockMvc.perform(get("/admin/credentials").session(sessionWithRole("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mockMvc.perform(post("/admin/credentials/" + UUID.randomUUID() + "/approve")
                        .session(sessionWithRole("ADMIN")).param("version", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/credentials/" + UUID.randomUUID() + "/approve")
                        .session(sessionWithRole("ADMIN")).param("version", "1").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminSubjectProposalRoutesRequireAdminSessionAndCsrfForMutations() throws Exception {
        mockMvc.perform(get("/admin/subjects"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/admin/subjects").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.when(adminApprovalService.subjectProposals(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        org.mockito.Mockito.when(subjects.getActiveSubjects()).thenReturn(List.of());
        mockMvc.perform(get("/admin/subjects").session(sessionWithRole("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mockMvc.perform(post("/admin/subjects/" + UUID.randomUUID() + "/reject")
                        .session(sessionWithRole("ADMIN")).param("reason", "duplicate").param("version", "0"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/subjects/" + UUID.randomUUID() + "/reject")
                        .session(sessionWithRole("ADMIN")).param("reason", "duplicate").param("version", "0").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminUserDirectoryRequiresAdminSessionAndCsrfForStatusChanges() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/admin/users").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.when(adminUserDirectoryService.findUsers(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        mockMvc.perform(get("/admin/users").session(sessionWithRole("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mockMvc.perform(post("/admin/users/" + UUID.randomUUID() + "/status")
                        .session(sessionWithRole("ADMIN")).param("status", "LOCKED").param("reason", "duplicate"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/" + UUID.randomUUID() + "/status")
                        .session(sessionWithRole("ADMIN")).param("status", "LOCKED").param("reason", "duplicate").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminRefundQueueRequiresAdminSessionAndCsrfForDecisions() throws Exception {
        mockMvc.perform(get("/admin/refunds"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/admin/refunds").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/refunds").session(sessionWithRole("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        String id = UUID.randomUUID().toString();
        var reject = post("/admin/refunds/" + id + "/reject")
                .param("reason", "Thông tin hoàn tiền không hợp lệ")
                .param("version", "1").param("idempotencyKey", UUID.randomUUID().toString());
        mockMvc.perform(reject.session(sessionWithRole("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(reject.session(sessionWithRole("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminExtensionQueueRequiresAdminSessionAndCsrfForDecisions() throws Exception {
        mockMvc.perform(get("/admin/extensions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/admin/extensions").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/extensions").session(sessionWithRole("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        var reject = post("/admin/extensions/" + UUID.randomUUID() + "/reject")
                .param("reason", "Không đủ điều kiện gia hạn")
                .param("idempotencyKey", UUID.randomUUID().toString());
        mockMvc.perform(reject.session(sessionWithRole("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(reject.session(sessionWithRole("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminPayoutQueueRequiresAdminSessionAndCsrfForDecisions() throws Exception {
        mockMvc.perform(get("/admin/payouts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/admin/payouts").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/payouts").session(sessionWithRole("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        var process = post("/admin/payouts/" + UUID.randomUUID() + "/process")
                .param("version", "1").param("idempotencyKey", UUID.randomUUID().toString());
        mockMvc.perform(process.session(sessionWithRole("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(process.session(sessionWithRole("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminSettlementQueueRequiresAdminSessionAndCsrfForDecisions() throws Exception {
        mockMvc.perform(get("/admin/booking-settlements"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/admin/booking-settlements").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/booking-settlements").session(sessionWithRole("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        var retain = post("/admin/booking-settlements/" + UUID.randomUUID() + "/retain")
                .param("version", "1").param("note", "Đã xem bằng chứng và quyết định giữ tiền");
        mockMvc.perform(retain.session(sessionWithRole("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(retain.session(sessionWithRole("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminSettingsAndAuditPagesRequireAdminSessionAndSettingsMutationCsrf() throws Exception {
        for (String route : List.of("/admin/settings", "/admin/audit-logs")) {
            mockMvc.perform(get(route)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/auth/login"));
            mockMvc.perform(get(route).session(sessionWithRole("STUDENT"))).andExpect(status().isForbidden());
            mockMvc.perform(get(route).session(sessionWithRole("ADMIN"))).andExpect(status().isOk());
        }
        var update = post("/admin/settings").param("commissionRate", "5.00")
                .param("bayesianMinimumReviews", "10").param("bookingReminderHours", "11")
                .param("bookingExpirationHours", "12");
        mockMvc.perform(update.session(sessionWithRole("ADMIN"))).andExpect(status().isForbidden());
        mockMvc.perform(update.session(sessionWithRole("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection());
    }

    @Test
    void htmlWorkspaceRejectsTheWrongSessionRole() throws Exception {
        mockMvc.perform(get("/teacher/profile")
                        .session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherPackagesRequireTeacherSessionAndCsrfForMutations() throws Exception {
        String packageId = UUID.randomUUID().toString();
        mockMvc.perform(get("/teacher/packages"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/packages")
                        .session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/packages/" + packageId + "/status")
                        .session(sessionWithRole("TEACHER"))
                        .param("version", "0").param("status", "INACTIVE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherAvailabilityRequiresTeacherSessionAndCsrfForReplacement() throws Exception {
        mockMvc.perform(get("/teacher/availability"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/availability").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/availability").session(sessionWithRole("TEACHER"))
                        .param("action", "save"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherStudentsRequireTeacherSession() throws Exception {
        mockMvc.perform(get("/teacher/students"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/students").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/teacher/students/{id}", UUID.randomUUID()).session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherBookingsRequireTeacherSessionAndCsrfForActions() throws Exception {
        String bookingId = UUID.randomUUID().toString();
        mockMvc.perform(get("/teacher/bookings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/bookings").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/teacher/bookings/" + bookingId).session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/bookings/" + bookingId + "/cancel")
                        .session(sessionWithRole("TEACHER")).param("version", "0").param("reason", "test"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/bookings/" + bookingId + "/cancel")
                        .session(sessionWithRole("TEACHER")).with(csrf()).param("version", "0").param("reason", "test"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void teacherTrialRequestsRequireTeacherSessionAndCsrfForResponses() throws Exception {
        String requestId = UUID.randomUUID().toString();
        mockMvc.perform(get("/teacher/trial-requests"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/trial-requests").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/trial-requests/" + requestId + "/reject")
                        .session(sessionWithRole("TEACHER")).param("version", "0").param("page", "0").param("reason", "Bận"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/trial-requests/" + requestId + "/reject")
                        .session(sessionWithRole("TEACHER")).with(csrf())
                        .param("version", "0").param("page", "0").param("reason", "Bận"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void teacherAssignmentsRequireTeacherSessionAndCsrfForCommands() throws Exception {
        String assignmentId = UUID.randomUUID().toString();
        mockMvc.perform(get("/teacher/assignments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/assignments").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/assignments/" + assignmentId + "/publish")
                        .session(sessionWithRole("TEACHER")).param("version", "0"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/assignments/" + assignmentId + "/publish")
                        .session(sessionWithRole("STUDENT")).param("version", "0")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentPackageWorkspaceRedirectsAnonymousAndRejectsOtherRoles() throws Exception {
        mockMvc.perform(get("/student/packages"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));

        mockMvc.perform(get("/student/checkout/" + UUID.randomUUID()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));

        mockMvc.perform(get("/student/payments/callback").param("invoiceId", UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));

        mockMvc.perform(get("/student/packages")
                        .session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/student/payment-result/" + UUID.randomUUID())
                        .session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentAssignmentsRequireStudentSessionAndCsrfOnSubmission() throws Exception {
        String assignmentId = UUID.randomUUID().toString();
        mockMvc.perform(get("/student/assignments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/student/assignments")
                        .session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/assignments/" + assignmentId + "/submission")
                        .session(sessionWithRole("STUDENT"))
                        .param("version", "0").param("status", "SUBMITTED").param("answer", "Answer"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionReportsRequireStudentSession() throws Exception {
        mockMvc.perform(get("/student/session-reports"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/student/session-reports")
                        .session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentProfileRequiresStudentSessionAndCsrfOnUpdate() throws Exception {
        mockMvc.perform(get("/student/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/student/profile").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/profile").session(sessionWithRole("STUDENT"))
                        .param("parentEmail", "parent@example.test"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/profile").session(sessionWithRole("STUDENT"))
                        .param("parentEmail", "parent@example.test")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void studentBookingPagesRequireStudentSessionAndCsrfForConfirmation() throws Exception {
        String bookingId = UUID.randomUUID().toString();
        mockMvc.perform(get("/student/bookings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/student/bookings").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/student/bookings/" + bookingId).session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/bookings/" + bookingId + "/confirm")
                        .session(sessionWithRole("STUDENT")).param("version", "0"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/bookings/" + bookingId + "/confirm")
                        .session(sessionWithRole("STUDENT")).param("version", "0")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void notificationPagesAreRoleProtectedAndMutationsRequireCsrf() throws Exception {
        String notificationId = UUID.randomUUID().toString();
        mockMvc.perform(get("/student/notifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/student/notifications").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/teacher/notifications").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/notifications/" + notificationId + "/read")
                        .session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/notifications/" + notificationId + "/read")
                        .session(sessionWithRole("STUDENT")).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void studentMessagesRequireStudentSessionAndCsrfForAllMutations() throws Exception {
        mockMvc.perform(get("/student/messages"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/student/messages").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/messages/send").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/messages/open").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/messages/" + UUID.randomUUID() + "/read")
                        .session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/messages/" + UUID.randomUUID() + "/read")
                        .session(sessionWithRole("STUDENT"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void teacherMessagesRequireTeacherSessionAndCsrfForMutations() throws Exception {
        mockMvc.perform(get("/teacher/messages"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/messages").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/messages/send").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/messages/" + UUID.randomUUID() + "/read")
                        .session(sessionWithRole("TEACHER"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void teacherStatsRequireTeacherSession() throws Exception {
        mockMvc.perform(get("/teacher/stats"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/stats").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherFinanceRequiresTeacherSessionAndCsrfForEveryMutation() throws Exception {
        mockMvc.perform(get("/teacher/wallet"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/bank-accounts").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/teacher/payouts").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/bank-accounts").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/payouts").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/bank-accounts").session(sessionWithRole("STUDENT"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
        String id = UUID.randomUUID().toString();
        String key = UUID.randomUUID().toString();
        var csrf = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf();
        mockMvc.perform(post("/teacher/bank-accounts").session(sessionWithRole("TEACHER")).with(csrf)
                        .param("bankBin", "970436").param("bankName", "Vietcombank")
                        .param("accountNumber", "123456789").param("accountHolderName", "NGUYEN AN")
                        .param("idempotencyKey", key))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/teacher/bank-accounts/{id}/update", id).session(sessionWithRole("TEACHER")).with(csrf)
                        .param("bankBin", "970436").param("bankName", "Vietcombank")
                        .param("accountNumber", "123456789").param("accountHolderName", "NGUYEN AN")
                        .param("version", "0"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/teacher/bank-accounts/{id}/delete", id).session(sessionWithRole("TEACHER")).with(csrf)
                        .param("version", "0"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/teacher/payouts").session(sessionWithRole("TEACHER")).with(csrf)
                        .param("bankAccountId", id).param("amountVnd", "1000")
                        .param("walletVersion", "0").param("idempotencyKey", key))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void teacherDashboardRequiresTeacherSession() throws Exception {
        mockMvc.perform(get("/teacher"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/teacher").session(sessionWithRole("TEACHER")))
                .andExpect(status().isOk());
    }

    @Test
    void teacherProfileRequiresTeacherSessionAndCsrfForUpdates() throws Exception {
        mockMvc.perform(get("/teacher/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/profile").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/profile/submit").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherDocumentsRequireTeacherSessionAndCsrfForMutations() throws Exception {
        mockMvc.perform(get("/teacher/documents"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/documents").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/documents/" + UUID.randomUUID() + "/delete")
                        .session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCredentialsRequireTeacherSessionAndCsrfForMutations() throws Exception {
        mockMvc.perform(get("/teacher/credentials"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/credentials").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/credentials" ).session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/credentials/" + UUID.randomUUID() + "/delete")
                        .session(sessionWithRole("TEACHER")).param("version", "0"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherSubjectsRequireTeacherSessionAndCsrfForMutations() throws Exception {
        mockMvc.perform(get("/teacher/subjects"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/subjects").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/subjects/assign").session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/subjects/" + UUID.randomUUID() + "/remove")
                        .session(sessionWithRole("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherSubjectProposalsRequireTeacherSessionAndCsrf() throws Exception {
        mockMvc.perform(get("/teacher/subject-proposals"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        mockMvc.perform(get("/teacher/subject-proposals").session(sessionWithRole("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/subject-proposals").session(sessionWithRole("TEACHER"))
                        .param("proposedName", "Vật lý ứng dụng").param("educationLevel", "HIGH_SCHOOL"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentRequestCreationRequiresStudentSessionAndCsrf() throws Exception {
        String packageId = UUID.randomUUID().toString();
        var request = post("/student/requests/refund")
                .param("studentPackageId", packageId).param("packageVersion", "1")
                .param("idempotencyKey", UUID.randomUUID().toString()).param("reason", "Không thể tiếp tục lịch học")
                .param("requestedSessions", "1").param("bankName", "Ngân hàng thử nghiệm")
                .param("bankBin", "970436").param("accountNumber", "1234567890")
                .param("accountHolderName", "NGUYEN VAN A");
        mockMvc.perform(request)
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/requests/refund")
                        .session(sessionWithRole("TEACHER"))
                        .param("studentPackageId", packageId).param("packageVersion", "1")
                        .param("idempotencyKey", UUID.randomUUID().toString()).param("reason", "Không thể tiếp tục lịch học")
                        .param("requestedSessions", "1").param("bankName", "Ngân hàng thử nghiệm")
                        .param("bankBin", "970436").param("accountNumber", "1234567890")
                        .param("accountHolderName", "NGUYEN VAN A")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/requests/refund")
                        .session(sessionWithRole("STUDENT"))
                        .param("studentPackageId", packageId).param("packageVersion", "1")
                        .param("idempotencyKey", UUID.randomUUID().toString()).param("reason", "Không thể tiếp tục lịch học")
                        .param("requestedSessions", "1").param("bankName", "Ngân hàng thử nghiệm")
                        .param("bankBin", "970436").param("accountNumber", "1234567890")
                        .param("accountHolderName", "NGUYEN VAN A"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/requests/refund")
                        .session(sessionWithRole("STUDENT"))
                        .param("studentPackageId", packageId).param("packageVersion", "1")
                        .param("idempotencyKey", UUID.randomUUID().toString()).param("reason", "Không thể tiếp tục lịch học")
                        .param("requestedSessions", "1").param("bankName", "Ngân hàng thử nghiệm")
                        .param("bankBin", "970436").param("accountNumber", "1234567890")
                        .param("accountHolderName", "NGUYEN VAN A")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void htmlMutationRequiresCsrfEvenOnPublicAuthPaths() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession sessionWithRole(String role) {
        var principal = new AuthenticatedUser(UUID.randomUUID(), "person@example.test", role);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
