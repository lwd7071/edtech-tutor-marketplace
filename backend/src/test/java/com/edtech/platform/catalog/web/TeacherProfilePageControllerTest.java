package com.edtech.platform.catalog.web;

import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.dto.PricingPackageView;
import com.edtech.platform.catalog.dto.TeacherPublicDetail;
import com.edtech.platform.catalog.service.PricingPackageService;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.ranking.facade.PublicReviewFacade;
import com.edtech.platform.ranking.facade.dto.PublicReviewSnapshot;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import com.edtech.platform.teacher.dto.AvailabilityView;
import com.edtech.platform.teacher.dto.PublicCredentialBadge;
import com.edtech.platform.teacher.facade.TeacherFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.edtech.platform.common.security.AuthenticatedUser;
import java.util.Collections;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherProfilePageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherProfilePageControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeacherMarketplaceService teacherMarketplaceService;

    @MockBean
    private PricingPackageService pricingPackageService;

    @MockBean
    private TeacherFacade teacherFacade;

    @MockBean
    private PublicReviewFacade reviewFacade;

    @MockBean
    private SubjectFacade subjectFacade;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserStatusCacheService userStatusCacheService;

    @MockBean
    private SessionService sessions;

    @MockBean
    private Clock clock;

    @BeforeEach
    void setUpClock() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-29T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void rendersPublicProfilePackagesAvailabilityAndReviews() throws Exception {
        UUID teacherId = UUID.randomUUID();
        TeacherPublicDetail teacher = new TeacherPublicDetail(
                teacherId, "Nguyễn Minh An", null, "Dạy Toán theo mục tiêu của học viên.",
                5, List.of("Tiếng Việt"), true, false, "Đà Nẵng", "Hải Châu", null,
                List.of("Toán"), 4.8, 4.8, 1, 4, List.of(new PublicCredentialBadge(UUID.randomUUID(), "Đã xác minh bằng cấp")));
        PricingPackageView pricingPackage = new PricingPackageView(
                UUID.randomUUID(), UUID.randomUUID(), "Toán", "Gói Toán nền tảng",
                "Ôn chắc kiến thức theo mục tiêu.", 12, 90, 1800000, 60, PackageStatus.ACTIVE, 0);
        AvailabilityView slot = new AvailabilityView(
                UUID.randomUUID(), DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Asia/Ho_Chi_Minh", true);
        PublicReviewSnapshot review = new PublicReviewSnapshot(
                UUID.randomUUID(), (short) 5, "Giải thích rõ và dễ theo dõi.", ZonedDateTime.now(), "Học viên", null);

        when(teacherMarketplaceService.getTeacherDetail(teacherId)).thenReturn(teacher);
        when(pricingPackageService.getTeacherPackages(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(pricingPackage)));
        when(teacherFacade.getPublicAvailability(teacherId)).thenReturn(List.of(slot));
        when(reviewFacade.getPublicReviews(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(review)));

        mockMvc.perform(get("/teachers/{id}", teacherId))
                .andExpect(status().isOk())
                .andExpect(view().name("teachers/profile"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Minh An")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gói Toán nền tảng")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thứ hai")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Giải thích rõ và dễ theo dõi.")));
    }

    @Test
    void publicProfilePackageShouldRenderTwoColumnHorizontalLayoutWithContentAndAction() throws Exception {
        UUID teacherId = UUID.randomUUID();
        TeacherPublicDetail teacher = new TeacherPublicDetail(
                teacherId, "Thầy Nguyễn Văn Toán", null, "Chuyên luyện thi đại số và hình học.",
                5, List.of("Tiếng Việt"), true, false, "Hà Nội", "Cầu Giấy", null,
                List.of("Toán"), 4.9, 4.9, 1, 10, List.of(new PublicCredentialBadge(UUID.randomUUID(), "Đã xác minh bằng cấp")));
        PricingPackageView pricingPackage = new PricingPackageView(
                UUID.randomUUID(), UUID.randomUUID(), "Toán Học", "Toán Nền Tảng 10 Buổi",
                "Lấy lại kiến thức cơ bản đại số & hình học lớp 12", 10, 60, 2000000, 60, PackageStatus.ACTIVE, 0);

        when(teacherMarketplaceService.getTeacherDetail(teacherId)).thenReturn(teacher);
        when(pricingPackageService.getTeacherPackages(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(pricingPackage)));
        when(teacherFacade.getPublicAvailability(teacherId)).thenReturn(List.of());
        when(reviewFacade.getPublicReviews(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/teachers/{id}", teacherId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"profile-package\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"profile-package__content\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"profile-package__action\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Toán Nền Tảng 10 Buổi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2.000.000đ")));
    }

    @Test
    void rendersNotFoundPageForMissingTeacher() throws Exception {
        UUID teacherId = UUID.randomUUID();
        when(teacherMarketplaceService.getTeacherDetail(teacherId))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/teachers/{id}", teacherId))
                .andExpect(status().isNotFound())
                .andExpect(view().name("teachers/not-found"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không tìm thấy hồ sơ gia sư")));
    }

    @Test
    void mapsMissingTeacherProfileFromTeacherFacadeToNotFoundPage() throws Exception {
        UUID teacherId = UUID.randomUUID();
        when(teacherMarketplaceService.getTeacherDetail(teacherId))
                .thenThrow(new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        mockMvc.perform(get("/teachers/{id}", teacherId))
                .andExpect(status().isNotFound())
                .andExpect(view().name("teachers/not-found"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không tìm thấy hồ sơ gia sư")));
    }

    @Test
    void studentCanOpenMessagesFromProfileAndGuestGetsLoginLink() throws Exception {
        UUID teacherId = UUID.randomUUID();
        PricingPackageView pricingPackage = new PricingPackageView(
                UUID.randomUUID(), UUID.randomUUID(), "Toán", "Gói Toán", "Mô tả", 8, 90, 800000, 30, PackageStatus.ACTIVE, 0);
        when(teacherMarketplaceService.getTeacherDetail(teacherId)).thenReturn(new TeacherPublicDetail(
                teacherId, "Gia sư", null, "Giới thiệu", 1, List.of(), true, false, null, null, null,
                List.of(), 0, 0, 0, 0, List.of()));
        when(pricingPackageService.getTeacherPackages(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(pricingPackage)));
        when(teacherFacade.getPublicAvailability(teacherId)).thenReturn(List.of());
        when(reviewFacade.getPublicReviews(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of()));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(UUID.randomUUID(), "student@example.test", "STUDENT"), null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
        try {
            mockMvc.perform(get("/teachers/{id}", teacherId))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/student/messages/open\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"button button--secondary\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"teacherId\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/student/invoices\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(pricingPackage.id().toString())))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"idempotencyKey\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Không gian của tôi")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/auth/logout\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Đăng nhập"))));
        } finally {
            SecurityContextHolder.clearContext();
        }

        mockMvc.perform(get("/teachers/{id}", teacherId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/auth/login?continueTo=")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("packageId%3D" + pricingPackage.id())));
    }

    @Test
    void studentSeesNativeTrialRequestFormForAssignedActiveSubjects() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        when(teacherMarketplaceService.getTeacherDetail(teacherId)).thenReturn(new TeacherPublicDetail(
                teacherId, "Gia sư", null, "Giới thiệu", 1, List.of(), true, false, null, null, null,
                List.of("Toán"), 0, 0, 0, 0, List.of()));
        when(pricingPackageService.getTeacherPackages(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(teacherFacade.getPublicAvailability(teacherId)).thenReturn(List.of());
        when(teacherFacade.getSubjectIdsForTeacher(teacherId)).thenReturn(List.of(subjectId));
        when(subjectFacade.getSubjects(List.of(subjectId))).thenReturn(java.util.Map.of(subjectId,
                new SubjectSnapshot(subjectId, "MATH", "Toán", "SECONDARY", true)));
        when(reviewFacade.getPublicReviews(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(UUID.randomUUID(), "student@example.test", "STUDENT"), null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
        try {
            mockMvc.perform(get("/teachers/{id}", teacherId))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/student/trials/requests\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"subjectId\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(subjectId.toString())))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"preferredStartTime\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"note\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Thời gian gửi là mong muốn ban đầu")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
