package com.edtech.platform.catalog.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.dto.PricingPackageView;
import com.edtech.platform.catalog.service.PricingPackageService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.teacher.dto.TeacherSubjectView;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import com.edtech.platform.teacher.service.TeacherSubjectService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherPackagesPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherPackagesPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private PricingPackageService packages;
    @MockBean private TeacherSubjectService subjects;
    @MockBean private EnrollmentFacade enrollments;
    @MockBean private TeacherFacade teachers;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;
    private final UUID teacherId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID();
    private final PricingPackageView view = new PricingPackageView(packageId, subjectId, "Toán học", "Gói ôn tập", "Mô tả",
            10, 90, 1_500_000, 60, PackageStatus.ACTIVE, 3);

    @BeforeEach
    void authenticateTeacher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
        when(subjects.getSubjects(teacherId)).thenReturn(List.of(new TeacherSubjectView(UUID.randomUUID(),
                new TeacherSubjectView.SubjectDto(subjectId, "MATH", "Toán học", "HIGH_SCHOOL"), null, null, true)));
        when(teachers.getTeacherByUserId(teacherId)).thenReturn(new TeacherSnapshot(UUID.randomUUID(), teacherId,
                "APPROVED", true, true, "Gia sư", null, null, 2, true, false, List.of(), null, null));
    }

    @Test
    void listRendersPackageAndUsesPrivateNoStoreResponse() throws Exception {
        when(packages.getOwnPackages(teacherId, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of(view)));
        mockMvc.perform(get("/teacher/packages"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-packages"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gói ôn tập")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1.500.000 ₫")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đang bán")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"skip-link\" href=\"#main-content\"")));
    }

    @Test
    void outOfRangePageRedirectsToLastPageInsteadOfShowingFalseEmptyState() throws Exception {
        PageRequest requested = PageRequest.of(99, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(packages.getOwnPackages(teacherId, requested)).thenReturn(new PageImpl<>(List.of(), requested, 2));

        mockMvc.perform(get("/teacher/packages").param("page", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/packages?page=0"));
    }

    @Test
    void createPageShowsOnlyAssignedSubjectsAndNativeForm() throws Exception {
        mockMvc.perform(get("/teacher/packages/create"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-package-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/teacher/packages/create")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Toán học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("method=\"post\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void unapprovedTeacherCannotSubmitPackageOrChooseActiveStatus() throws Exception {
        when(teachers.getTeacherByUserId(teacherId)).thenReturn(new TeacherSnapshot(UUID.randomUUID(), teacherId,
                "PENDING", false, false, "Gia sư", null, null, 2, true, false, List.of(), null, null));
        mockMvc.perform(get("/teacher/packages/create"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hồ sơ gia sư cần được duyệt")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("disabled=\"disabled\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<option value=\"ACTIVE\">"))));
    }

    @Test
    void createValidPackagePostsAndRedirectsToOwnedDetail() throws Exception {
        when(packages.createPackage(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(view);
        mockMvc.perform(post("/teacher/packages/create").with(csrf()).param("subjectId", subjectId.toString())
                        .param("name", "  Gói ôn tập  ").param("description", "  Mô tả  ")
                        .param("totalSessions", "10").param("durationDays", "90").param("priceVnd", "1500000")
                        .param("sessionDurationMinutes", "60").param("status", "DRAFT"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/teacher/packages/" + packageId))
                .andExpect(flash().attribute("packageNotice", "Đã tạo gói học."));
    }

    @Test
    void invalidCreateRetainsFormAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/teacher/packages/create").with(csrf()).param("subjectId", "")
                        .param("name", "").param("totalSessions", "0").param("durationDays", "-1")
                        .param("priceVnd", "0").param("sessionDurationMinutes", "0").param("status", "DRAFT"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-package-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Có thông tin chưa hợp lệ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chọn môn học.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"0\"")));
    }

    @Test
    void detailLocksContentWhenPurchasedAndOnlyOffersDeactivate() throws Exception {
        when(packages.getOwnPackage(teacherId, packageId)).thenReturn(view);
        when(enrollments.hasStudentPackage(packageId)).thenReturn(true);
        mockMvc.perform(get("/teacher/packages/{id}", packageId))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-package-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gói đã có học viên mua")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tạm ngưng bán")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Bật bán"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("disabled=\"disabled\"")));
    }

    @Test
    void purchaseStateReadFailureFailsClosed() throws Exception {
        when(packages.getOwnPackage(teacherId, packageId)).thenReturn(view);
        when(enrollments.hasStudentPackage(packageId)).thenThrow(new IllegalStateException("private failure"));
        mockMvc.perform(get("/teacher/packages/{id}", packageId))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa xác minh được trạng thái mua")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("disabled=\"disabled\"")));
    }

    @Test
    void updateSubmitsVersionAndUsesPrg() throws Exception {
        when(packages.getOwnPackage(teacherId, packageId)).thenReturn(view);
        when(enrollments.hasStudentPackage(packageId)).thenReturn(false);
        mockMvc.perform(post("/teacher/packages/{id}/update", packageId).with(csrf())
                        .param("subjectId", subjectId.toString()).param("name", "Gói mới").param("totalSessions", "8")
                        .param("durationDays", "60").param("priceVnd", "1200000").param("sessionDurationMinutes", "45")
                        .param("status", "ACTIVE").param("version", "3"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/teacher/packages/" + packageId))
                .andExpect(flash().attribute("packageNotice", "Đã cập nhật gói học."));
        verify(packages).updatePackage(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.eq(packageId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateValidationErrorKeepsInputAndMapsSafeBusinessError() throws Exception {
        when(packages.getOwnPackage(teacherId, packageId)).thenReturn(view);
        when(enrollments.hasStudentPackage(packageId)).thenReturn(false);
        doThrow(new BusinessException(ErrorCode.CONCURRENT_MODIFICATION)).when(packages)
                .updatePackage(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.eq(packageId), org.mockito.ArgumentMatchers.any());
        mockMvc.perform(post("/teacher/packages/{id}/update", packageId).with(csrf())
                        .param("subjectId", subjectId.toString()).param("name", "Bản cập nhật chưa lưu").param("totalSessions", "2")
                        .param("durationDays", "1").param("priceVnd", "1000").param("sessionDurationMinutes", "45")
                        .param("status", "ACTIVE").param("version", "2"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-package-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gói vừa được thay đổi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bản cập nhật chưa lưu")));
    }

    @Test
    void statusChangeUsesPostedVersionAndPrg() throws Exception {
        mockMvc.perform(post("/teacher/packages/{id}/status", packageId).with(csrf())
                        .param("status", "INACTIVE").param("version", "3"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/teacher/packages/" + packageId))
                .andExpect(flash().attribute("packageNotice", "Đã cập nhật trạng thái gói."));
        verify(packages).changeStatus(teacherId, packageId,
                new com.edtech.platform.catalog.dto.ChangePackageStatusRequest(PackageStatus.INACTIVE, 3L));
    }
}
