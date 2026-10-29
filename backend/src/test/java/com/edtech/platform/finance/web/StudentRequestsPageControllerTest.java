package com.edtech.platform.finance.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.booking.dto.response.TrialRequestView;
import com.edtech.platform.booking.domain.TrialRequestStatus;
import com.edtech.platform.booking.service.TrialRequestService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.enrollment.dto.StudentOwnedPackageView;
import com.edtech.platform.enrollment.service.StudentPackageReadService;
import com.edtech.platform.finance.dto.response.RefundRequestView;
import com.edtech.platform.finance.service.ExtensionService;
import com.edtech.platform.finance.service.FinanceCommandExecutor;
import com.edtech.platform.finance.service.RefundService;
import com.edtech.platform.finance.dto.request.CreateRefundRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(StudentRequestsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentRequestsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private RefundService refunds;
    @MockBean private ExtensionService extensions;
    @MockBean private TrialRequestService trials;
    @MockBean private StudentPackageReadService packages;
    @MockBean private FinanceCommandExecutor financeCommands;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessionService;
    @MockBean private Clock clock;
    private final UUID studentId = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(clock.withZone(org.mockito.ArgumentMatchers.any())).thenReturn(Clock.fixed(Instant.parse("2026-10-28T00:00:00Z"), ZoneOffset.UTC));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(studentId, "student@example.test", "STUDENT"), null, List.of()));
    }

    @Test
    void rendersTrialRequestsWithNativePaginationAndNoScript() throws Exception {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        var trial = new TrialRequestView(UUID.randomUUID(), UUID.randomUUID(), studentId, UUID.randomUUID(),
                Instant.parse("2026-11-02T10:00:00Z"), "Muốn học thử", TrialRequestStatus.PENDING, null, null,
                null, Instant.parse("2026-10-27T10:00:00Z"), 0);
        when(trials.findForStudent(eq(studentId), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(trial), pageable, 11));

        mockMvc.perform(get("/student/requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/requests"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Học thử")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("#" + trial.id().toString().substring(0, 8))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trang sau")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("để gửi yêu cầu hoàn tiền hoặc gia hạn"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void rendersOwnedPackageRefundFormWithVersionAndIdempotencyKey() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-28T00:00:00Z"));
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(refunds.findStudentRefunds(eq(studentId), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));
        when(packages.detail(eq(packageId), eq(studentId))).thenReturn(Optional.of(new StudentOwnedPackageView(
                packageId, UUID.randomUUID(), UUID.randomUUID(), "Toán 11 — 10 buổi", 10, 4, 1, 5, 0,
                1000000, Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2027-01-01T00:00:00Z"),
                "ACTIVE", null, 7, new StudentOwnedPackageView.TeacherRef(UUID.randomUUID(), "Gia sư", null),
                new StudentOwnedPackageView.SubjectRef(UUID.randomUUID(), "Toán 11"))));

        mockMvc.perform(get("/student/requests").param("tab", "refund").param("packageId", packageId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Yêu cầu hoàn tiền")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"packageVersion\" value=\"7\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"idempotencyKey\"")));
    }

    @Test
    void rendersEmptyAndUnavailableRequestLists() throws Exception {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(extensions.findStudentExtensions(eq(studentId), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));
        mockMvc.perform(get("/student/requests").param("tab", "extension"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có yêu cầu gia hạn")));

        when(extensions.findStudentExtensions(eq(studentId), eq(pageable))).thenThrow(new IllegalStateException("unavailable"));
        mockMvc.perform(get("/student/requests").param("tab", "extension"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh sách yêu cầu")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thử tải lại")));
    }

    @Test
    void refundPostUsesFinanceIdempotencyExecutorAndRedirectsWithoutReplayingSecrets() throws Exception {
        UUID key = UUID.randomUUID();
        doAnswer(invocation -> {
            ((Supplier<?>) invocation.getArgument(5)).get();
            return null;
        }).when(financeCommands).execute(eq(studentId), eq("STUDENT_REFUND_CREATE"), eq(key),
                any(CreateRefundRequest.class), eq(RefundRequestView.class), any());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/student/requests/refund")
                        .param("studentPackageId", packageId.toString()).param("packageVersion", "7")
                        .param("idempotencyKey", key.toString()).param("reason", "Không thể tiếp tục lịch học")
                        .param("requestedSessions", "2").param("bankName", "Ngân hàng thử nghiệm")
                        .param("bankBin", "970436").param("accountNumber", "1234567890")
                        .param("accountHolderName", "nguyen van a"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/requests?tab=refund"));

        verify(financeCommands).execute(eq(studentId), eq("STUDENT_REFUND_CREATE"), eq(key),
                any(CreateRefundRequest.class), eq(RefundRequestView.class), any());
    }
}
