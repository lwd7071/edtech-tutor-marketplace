package com.edtech.platform.enrollment.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.enrollment.domain.StudentPackageStatus;
import com.edtech.platform.enrollment.dto.StudentOwnedPackageView;
import com.edtech.platform.enrollment.service.StudentPackageReadService;
import com.edtech.platform.booking.service.BookingRequestService;
import com.edtech.platform.booking.dto.request.CreateBookingRequestCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StudentPackagePageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentPackagePageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private StudentPackageReadService packages;
    @MockBean private BookingRequestService bookingRequests;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessionService;
    @MockBean private Clock clock;

    private final UUID studentId = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID();
    private final UsernamePasswordAuthenticationToken student = new UsernamePasswordAuthenticationToken(
            new AuthenticatedUser(studentId, "student@example.com", "STUDENT"), null, List.of());

    @BeforeEach
    void authenticateStudent() {
        SecurityContextHolder.getContext().setAuthentication(student);
    }

    @Test
    void rendersOwnedPackagesAndNativeGetFilterAndPagination() throws Exception {
        var owned = packageView(packageId, "ACTIVE");
        when(packages.list(eq(studentId), eq(StudentPackageStatus.ACTIVE), eq(PageRequest.of(0, 9))))
                .thenReturn(new PageImpl<>(List.of(owned), PageRequest.of(0, 9), 12));

        mockMvc.perform(get("/student/packages").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/packages"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gói học của tôi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Toán lớp 10 — 12 buổi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("3.600.000 ₫")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/student/packages/" + packageId)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("status=ACTIVE")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trang 1 / 2")));
        verify(packages).list(studentId, StudentPackageStatus.ACTIVE, PageRequest.of(0, 9));
    }

    @Test
    void rendersEmptyStateAndInvalidFilterWithoutLosingThePage() throws Exception {
        when(packages.list(eq(studentId), isNull(), eq(PageRequest.of(0, 9))))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 9), 0));

        mockMvc.perform(get("/student/packages").param("status", "NOT_A_STATUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/packages"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trạng thái lọc không hợp lệ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bạn chưa có gói học nào.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("method=\"get\"")));
    }

    @Test
    void rendersUnavailableStateWhenPackageReadFails() throws Exception {
        when(packages.list(eq(studentId), isNull(), eq(PageRequest.of(0, 9))))
                .thenThrow(new IllegalStateException("unavailable"));

        mockMvc.perform(get("/student/packages"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh sách gói học.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thử tải lại")));
    }

    @Test
    void redirectsAnOutOfRangePageToTheLastAvailablePage() throws Exception {
        when(packages.list(eq(studentId), eq(StudentPackageStatus.ACTIVE), eq(PageRequest.of(4, 9))))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(4, 9), 12));

        mockMvc.perform(get("/student/packages").param("status", "ACTIVE").param("page", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl(
                        "/student/packages?page=1&status=ACTIVE"));
    }

    @Test
    void rendersPackageDetailFromStudentOwnedRead() throws Exception {
        when(packages.detail(packageId, studentId)).thenReturn(Optional.of(packageView(packageId, "ACTIVE")));

        mockMvc.perform(get("/student/packages/{id}", packageId))
                .andExpect(status().isOk())
                .andExpect(view().name("student/package-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Phân bổ buổi học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Còn 6 / 12 buổi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("01/09/2026")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/student/requests?tab=refund&amp;packageId=" + packageId)));
    }

    @Test
    void rendersBookingRequestAsNativePostFormOnActivePackage() throws Exception {
        when(packages.detail(packageId, studentId)).thenReturn(Optional.of(packageView(packageId, "ACTIVE")));

        mockMvc.perform(get("/student/packages/{id}", packageId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Yêu cầu đặt buổi học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"datetime-local\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("method=\"post\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"studentNote\"")));
    }

    @Test
    void submitsBookingRequestWithVietnameseLocalTimeAsNativePost() throws Exception {
        when(bookingRequests.create(eq(studentId), any(CreateBookingRequestCommand.class)))
                .thenReturn(null);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/student/packages/{id}/booking-request", packageId)
                        .param("preferredStartTime", "2026-11-01T19:30")
                        .param("studentNote", "Buổi tối giúp mình nhé"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/student/packages/" + packageId));
        verify(bookingRequests).create(eq(studentId), org.mockito.ArgumentMatchers.argThat(command ->
                command.preferredStartTime().equals(Instant.parse("2026-11-01T12:30:00Z"))
                        && command.studentNote().equals("Buổi tối giúp mình nhé")));
    }

    @Test
    void exposesExtensionRequestOnlyForExpiredOwnedPackage() throws Exception {
        when(packages.detail(packageId, studentId)).thenReturn(Optional.of(packageView(packageId, "LOCKED_EXPIRED")));

        mockMvc.perform(get("/student/packages/{id}", packageId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/student/requests?tab=refund&amp;packageId=" + packageId)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/student/requests?tab=extension&amp;packageId=" + packageId)));
    }

    @Test
    void returnsNotFoundWhenPackageIsNotOwnedByTheStudent() throws Exception {
        when(packages.detail(packageId, studentId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/student/packages/{id}", packageId))
                .andExpect(status().isNotFound());
    }

    private StudentOwnedPackageView packageView(UUID id, String status) {
        return new StudentOwnedPackageView(id, UUID.randomUUID(), UUID.randomUUID(), "Toán lớp 10 — 12 buổi",
                12, 6, 1, 4, 1, 3_600_000,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2027-09-01T00:00:00Z"), status,
                null, 3,
                new StudentOwnedPackageView.TeacherRef(UUID.randomUUID(), "Nguyễn Thu Hà", null),
                new StudentOwnedPackageView.SubjectRef(UUID.randomUUID(), "Toán học"));
    }
}
