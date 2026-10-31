package com.edtech.platform.catalog.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD: Kiểm tra toàn bộ hệ thống button, tokens và quy tắc đồng bộ nút vàng chữ đen (trừ các chỗ đặc biệt có nền vàng).
 */
class ButtonSystemLayoutTest {

    @Test
    @DisplayName("tokens.css phải định nghĩa --radius-control là 8px theo chuẩn bo góc hiện đại")
    void tokensCssShouldDefineModernControlBorderRadius() throws Exception {
        Path tokensPath = Path.of("src/main/resources/static/css/tokens.css");
        assertTrue(Files.exists(tokensPath), "tokens.css must exist");
        String css = Files.readString(tokensPath);

        assertTrue(
                css.contains("--radius-control: 8px;"),
                "--radius-control must be upgraded to 8px for modern Neubrutalism-lite feel"
        );
        assertFalse(
                css.contains("--radius-control: 4px;"),
                "--radius-control: 4px is outdated and must be replaced"
        );
    }

    @Test
    @DisplayName("public.css: .button--primary phải có viền mực 1.5px và thiết kế phẳng, không có bóng đổ ở dưới")
    void primaryButtonShouldHaveCrispInkBorderAndNoBottomShadow() throws Exception {
        Path publicCssPath = Path.of("src/main/resources/static/css/public.css");
        assertTrue(Files.exists(publicCssPath), "public.css must exist");
        String css = Files.readString(publicCssPath);

        assertTrue(
                css.contains("border: 1.5px solid var(--ink)") || css.contains("border: 1.5px solid #171714"),
                ".button--primary must have crisp 1.5px ink border"
        );
        assertFalse(
                css.contains("box-shadow: 0 2px 0 var(--ink)") || css.contains("box-shadow: 0 2px 0 #171714"),
                ".button--primary must be flat with no bottom shadow"
        );
    }

    @Test
    @DisplayName("public.css: .button--secondary phải có viền 1.5px mực ink tương phản sắc nét theo phong cách Neubrutalism-lite")
    void secondaryButtonShouldHaveCrispBorder() throws Exception {
        Path publicCssPath = Path.of("src/main/resources/static/css/public.css");
        assertTrue(Files.exists(publicCssPath), "public.css must exist");
        String css = Files.readString(publicCssPath);

        assertTrue(
                css.contains(".button--secondary") && (css.contains("border: 1.5px solid var(--ink)") || css.contains("border: 1.5px solid #171714")),
                ".button--secondary must have a crisp 1.5px ink border"
        );
        assertFalse(
                css.contains("border: 1.5px solid var(--border)"),
                ".button--secondary must not use washed-out grey border"
        );
    }

    @Test
    @DisplayName("home.css: card thẻ gia sư, môn học và khối join phải có viền mực 1.5px sắc nét thay vì viền xám mờ")
    void homeCardsShouldHaveCrispInkBorder() throws Exception {
        Path homeCssPath = Path.of("src/main/resources/static/css/home.css");
        assertTrue(Files.exists(homeCssPath), "home.css must exist");
        String css = Files.readString(homeCssPath);

        // Teacher card co vien muc 1.5px
        assertTrue(
                css.contains(".teacher-card {") && (css.contains("border: 1.5px solid var(--ink)") || css.contains("border: 1.5px solid #171714")),
                ".teacher-card must have crisp 1.5px ink border"
        );
        assertFalse(
                css.contains(".teacher-card { min-width: 0; padding: 20px; background: var(--surface); border: 1px solid var(--border);"),
                ".teacher-card must not use faint 1px border"
        );

        // Home subject card co vien muc 1.5px
        assertTrue(
                css.contains(".home-subject {") && (css.contains("border: 1.5px solid var(--ink)") || css.contains("border: 1.5px solid #171714")),
                ".home-subject must have crisp 1.5px ink border"
        );

        // Home join block co vien muc 1.5px
        assertTrue(
                css.contains(".home-join {") && (css.contains("border: 1.5px solid var(--ink)") || css.contains("border: 1.5px solid #171714")),
                ".home-join must have crisp 1.5px ink border"
        );
    }

    @Test
    @DisplayName("home/index.html: section home-join phải đồng bộ dùng nút vàng chữ đen button--primary thay vì nút đen")
    void homePageJoinSectionShouldUsePrimaryYellowButton() throws Exception {
        Path homeHtmlPath = Path.of("src/main/resources/templates/home/index.html");
        assertTrue(Files.exists(homeHtmlPath), "home/index.html must exist");
        String html = Files.readString(homeHtmlPath);

        assertTrue(
                html.contains("class=\"button button--primary\" href=\"/become-a-tutor\""),
                "home-join CTA must use standardized button button--primary (yellow with black text)"
        );
        assertFalse(
                html.contains("class=\"home-button\" href=\"/become-a-tutor\""),
                "home-join must not use black home-button since its background is surface-warm, not yellow"
        );
    }

    @Test
    @DisplayName("home/index.html: thẻ gia sư phải có cụm nút kép Xem hồ sơ và Nhắn tin đồng bộ với search.html")
    void homePageTeacherCardShouldRenderDualActionButtonStack() throws Exception {
        Path homeHtmlPath = Path.of("src/main/resources/templates/home/index.html");
        assertTrue(Files.exists(homeHtmlPath), "home/index.html must exist");
        String html = Files.readString(homeHtmlPath);

        assertTrue(
                html.contains("class=\"teacher-card__actions\""),
                "Home page teacher card must render teacher-card__actions container"
        );
        assertFalse(
                html.contains("class=\"teacher-card__link\""),
                "Home page teacher card must not use outdated plain text teacher-card__link"
        );
    }

    @Test
    @DisplayName("auth.css: nút submit form auth phải đồng bộ viền 1.5px ink và phẳng hoàn toàn không có bóng đổ")
    void authSubmitButtonShouldUseCrispInkBorder() throws Exception {
        Path authCssPath = Path.of("src/main/resources/static/css/auth.css");
        assertTrue(Files.exists(authCssPath), "auth.css must exist");
        String css = Files.readString(authCssPath);

        assertTrue(
                css.contains("border: 1.5px solid var(--ink)") || css.contains("border: 1.5px solid #171714"),
                ".auth-submit must have crisp 1.5px ink border"
        );
        assertFalse(
                css.contains("box-shadow: 0 2px 0 var(--ink)") || css.contains("box-shadow: 0 2px 0 #171714"),
                ".auth-submit must not have bottom shadow"
        );
    }

    @Test
    @DisplayName("auth.css: .auth-submit và .auth-oauth phải căn giữa chữ (justify-content: center, gap: 8px) thay vì dạt hai bên")
    void authButtonsShouldCenterAlignContent() throws Exception {
        Path authCssPath = Path.of("src/main/resources/static/css/auth.css");
        assertTrue(Files.exists(authCssPath), "auth.css must exist");
        String css = Files.readString(authCssPath);

        assertTrue(
                css.contains(".auth-submit {") && css.contains("justify-content: center;"),
                ".auth-submit must center its label and arrow using justify-content: center"
        );
        assertTrue(
                css.contains(".auth-submit {") && (css.contains("gap: 8px;") || css.contains("gap: 10px;")),
                ".auth-submit must have balanced gap between text label and trailing arrow"
        );
        assertFalse(
                css.contains(".auth-submit { display: flex; align-items: center; justify-content: space-between;"),
                ".auth-submit must not use justify-content: space-between which pushes text and icon to edges"
        );
    }

    @Test
    @DisplayName("student-packages.css: nút Áp dụng lọc gói học phải là nút vàng chữ đen viền 1.5px, không dùng nền đen")
    void studentPackagesFilterSubmitMustUsePrimaryYellowButton() throws Exception {
        Path packagesCssPath = Path.of("src/main/resources/static/css/student-packages.css");
        assertTrue(Files.exists(packagesCssPath), "student-packages.css must exist");
        String css = Files.readString(packagesCssPath);

        assertTrue(
                css.contains(".package-filter__submit") && css.contains("background: var(--brand-yellow)"),
                ".package-filter__submit must use yellow background"
        );
        assertTrue(
                css.contains("border: 1.5px solid var(--ink)") || css.contains("border: 1.5px solid #171714"),
                ".package-filter__submit must have 1.5px ink border"
        );
        assertFalse(
                css.contains(".package-filter__submit { min-height: 42px; padding: 0 18px; border: 0; border-radius: var(--radius-control); background: var(--ink);"),
                ".package-filter__submit must not use outdated black background"
        );
    }

    @Test
    @DisplayName("assignments.html: nút Lọc bài tập phải là learning-button--primary và student-learning.css có viền 1.5px")
    void studentAssignmentsFilterSubmitMustUsePrimaryButton() throws Exception {
        Path assignmentsHtmlPath = Path.of("src/main/resources/templates/student/assignments.html");
        assertTrue(Files.exists(assignmentsHtmlPath), "assignments.html must exist");
        String html = Files.readString(assignmentsHtmlPath);

        assertTrue(
                html.contains("class=\"learning-button learning-button--primary\" type=\"submit\">Lọc bài tập</button>"),
                "Filter submit in assignments.html must use learning-button--primary (yellow CTA)"
        );
        assertFalse(
                html.contains("class=\"learning-button learning-button--secondary\" type=\"submit\">Lọc bài tập</button>"),
                "Filter submit in assignments.html must not use secondary white button"
        );

        Path learningCssPath = Path.of("src/main/resources/static/css/student-learning.css");
        assertTrue(Files.exists(learningCssPath), "student-learning.css must exist");
        String css = Files.readString(learningCssPath);

        assertTrue(
                css.contains(".learning-button--primary") && (css.contains("border: 1.5px solid var(--ink)") || css.contains("border:1.5px solid var(--ink)") || css.contains("border: 1.5px solid #171714")),
                ".learning-button--primary must have crisp 1.5px ink border"
        );
    }

    @Test
    @DisplayName("student-bookings.css: nút Lọc lịch và Xác nhận buổi học phải có viền mực 1.5px và phẳng hoàn toàn không có bóng đổ")
    void studentBookingsFilterButtonMustHaveCrispInkBorder() throws Exception {
        Path bookingsCssPath = Path.of("src/main/resources/static/css/student-bookings.css");
        assertTrue(Files.exists(bookingsCssPath), "student-bookings.css must exist");
        String css = Files.readString(bookingsCssPath);

        assertTrue(
                css.contains("border: 1.5px solid var(--ink)") || css.contains("border:1.5px solid var(--ink)") || css.contains("border: 1.5px solid #171714"),
                "Bookings buttons must have 1.5px ink border"
        );
        assertFalse(
                css.contains("box-shadow: 0 2px 0 var(--ink)") || css.contains("box-shadow:0 2px 0 var(--ink)") || css.contains("box-shadow: 0 2px 0 #171714"),
                "Bookings buttons must not have bottom shadow"
        );
    }

    @Test
    @DisplayName("Teacher Workspace CSS: các nút chính phải phẳng hoàn toàn và có viền 1.5px solid ink")
    void teacherButtonsShouldBeFlatAndUseStandardTokens() throws Exception {
        Path teacherBookingsCss = Path.of("src/main/resources/static/css/teacher-bookings.css");
        assertTrue(Files.exists(teacherBookingsCss), "teacher-bookings.css must exist");
        String bookingsCss = Files.readString(teacherBookingsCss);
        assertTrue(bookingsCss.contains("border: 1.5px solid") || bookingsCss.contains("border:1.5px solid"));
        assertFalse(bookingsCss.contains("box-shadow: 0 2px 0"));

        Path teacherPackagesCss = Path.of("src/main/resources/static/css/teacher-packages.css");
        assertTrue(Files.exists(teacherPackagesCss), "teacher-packages.css must exist");
        String packagesCss = Files.readString(teacherPackagesCss);
        assertTrue(packagesCss.contains("border: 1.5px solid") || packagesCss.contains("border:1.5px solid"));
        assertFalse(packagesCss.contains("box-shadow: 0 2px 0"));
    }

    @Test
    @DisplayName("Admin Workspace CSS: các nút filter/action phải dùng nền vàng và viền 1.5px ink phẳng hoàn toàn")
    void adminButtonsShouldBeYellowWithInkBorderAndFlat() throws Exception {
        Path adminUsersCss = Path.of("src/main/resources/static/css/admin-users.css");
        assertTrue(Files.exists(adminUsersCss), "admin-users.css must exist");
        String usersCss = Files.readString(adminUsersCss);
        assertTrue(usersCss.contains("background:var(--brand-yellow)") || usersCss.contains("background: var(--brand-yellow)"));
        assertTrue(usersCss.contains("border:1.5px solid var(--ink)") || usersCss.contains("border: 1.5px solid var(--ink)"));

        Path adminExtensionsCss = Path.of("src/main/resources/static/css/admin-extensions.css");
        assertTrue(Files.exists(adminExtensionsCss), "admin-extensions.css must exist");
        String extensionsCss = Files.readString(adminExtensionsCss);
        assertTrue(extensionsCss.contains("background:var(--brand-yellow)") || extensionsCss.contains("background: var(--brand-yellow)"));
        assertTrue(extensionsCss.contains("border:1.5px solid var(--ink)") || extensionsCss.contains("border: 1.5px solid var(--ink)"));
    }

    @Test
    @DisplayName("auth.css: logo, text links và radio role phải đồng bộ màu đen ink, không sử dụng màu cam warning")
    void authPagesShouldUseInkColorForBrandLinksAndRadioInsteadOfWarning() throws Exception {
        Path authCssPath = Path.of("src/main/resources/static/css/auth.css");
        assertTrue(Files.exists(authCssPath), "auth.css must exist");
        String css = Files.readString(authCssPath);

        // Logo match khong dung warning ma dong bo ink nhu landing page
        assertFalse(
                css.contains(".auth-brand span { color: var(--warning); }") || css.contains(".auth-brand span{color:var(--warning)}"),
                "Logo .auth-brand span must not use --warning color"
        );

        // Text links khong dung warning ma dung ink
        assertFalse(
                css.contains("a { color: var(--warning)") || css.contains("a{color:var(--warning)"),
                "Auth links must not use --warning color"
        );
        assertTrue(
                css.contains(".auth-label-row a, .auth-register a, .auth-back a, .auth-parent a") &&
                (css.contains("color: var(--ink)") || css.contains("color:var(--ink)")),
                "Auth links must use var(--ink) for high contrast and clean minimalist aesthetic"
        );

        // Radio role accent-color khong dung warning ma dung ink
        assertFalse(
                css.contains("accent-color: var(--warning)") || css.contains("accent-color:var(--warning)"),
                "Auth radio button must not use --warning accent-color"
        );
        assertTrue(
                css.contains("accent-color: var(--ink)") || css.contains("accent-color:var(--ink)"),
                "Auth radio button must use var(--ink) accent-color"
        );
    }

    @Test
    @DisplayName("home.css: thanh tìm kiếm, nút hero, nút header phải dùng box chữ nhật bo góc 8px/12px, không dùng pill 9999px")
    void homeHeroAndHeaderShouldUseStandardRectangularRoundedBordersInsteadOfPills() throws Exception {
        Path homeCssPath = Path.of("src/main/resources/static/css/home.css");
        assertTrue(Files.exists(homeCssPath), "home.css must exist");
        String css = Files.readString(homeCssPath);

        // Header button
        assertTrue(
                css.contains(".home-body .site-header .button--primary") &&
                (css.contains("border-radius: var(--radius-control)") || css.contains("border-radius: 8px") || css.contains("border-radius:var(--radius-control)")),
                "Header button on home page must use rectangular rounded border-radius (8px)"
        );

        // Hero search container
        assertTrue(
                css.contains(".home-search {") &&
                (css.contains("border-radius: var(--radius-card)") || css.contains("border-radius: 12px") || css.contains("border-radius:var(--radius-card)")),
                "Hero search container must use rectangular rounded border-radius (12px)"
        );

        // Hero search submit button
        assertTrue(
                css.contains(".home-button {") &&
                (css.contains("border-radius: var(--radius-control)") || css.contains("border-radius: 8px") || css.contains("border-radius:var(--radius-control)")),
                "Hero search button must use rectangular rounded border-radius (8px)"
        );

        // Hero badge
        assertTrue(
                css.contains(".home-hero__badge {") &&
                (css.contains("border-radius: var(--radius-control)") || css.contains("border-radius: 8px") || css.contains("border-radius:var(--radius-control)")),
                "Hero badge must use rectangular rounded border-radius (8px)"
        );
    }

    @Test
    @DisplayName("Public & Auth CSS: card và panel chính phải có viền mực 1.5px ink dứt khoát")
    void publicAndAuthCardsShouldHaveCrispInkBorder() throws Exception {
        // auth.css
        Path authCssPath = Path.of("src/main/resources/static/css/auth.css");
        assertTrue(Files.exists(authCssPath), "auth.css must exist");
        String authCss = Files.readString(authCssPath);
        assertTrue(
                authCss.contains(".auth-card {") && (authCss.contains("border: 1.5px solid var(--ink)") || authCss.contains("border: 1.5px solid #171714")),
                ".auth-card must have crisp 1.5px ink border"
        );
        assertTrue(
                authCss.contains(".auth-role {") && (authCss.contains("border: 1.5px solid var(--ink)") || authCss.contains("border: 1.5px solid #171714")),
                ".auth-role must have crisp 1.5px ink border"
        );

        // teachers.css (Search page)
        Path teachersCssPath = Path.of("src/main/resources/static/css/teachers.css");
        assertTrue(Files.exists(teachersCssPath), "teachers.css must exist");
        String teachersCss = Files.readString(teachersCssPath);
        assertTrue(
                teachersCss.contains(".teacher-card {") && (teachersCss.contains("border: 1.5px solid var(--ink)") || teachersCss.contains("border: 1.5px solid #171714")),
                ".teacher-card in teachers.css must have crisp 1.5px ink border"
        );

        // subjects.css
        Path subjectsCssPath = Path.of("src/main/resources/static/css/subjects.css");
        assertTrue(Files.exists(subjectsCssPath), "subjects.css must exist");
        String subjectsCss = Files.readString(subjectsCssPath);
        assertTrue(
                subjectsCss.contains(".subjects-filters") && (subjectsCss.contains("border: 1.5px solid var(--ink)") || subjectsCss.contains("border: 1.5px solid #171714") || subjectsCss.contains("border:1.5px solid var(--ink)")),
                ".subjects-filters must have crisp 1.5px ink border"
        );
    }

    @Test
    @DisplayName("Workspace CSS (Student/Teacher/Admin): card và panel chính phải có viền mực 1.5px ink dứt khoát")
    void workspaceCardsShouldHaveCrispInkBorder() throws Exception {
        // student-dashboard.css
        Path studentDashboardCssPath = Path.of("src/main/resources/static/css/student-dashboard.css");
        assertTrue(Files.exists(studentDashboardCssPath), "student-dashboard.css must exist");
        String studentCss = Files.readString(studentDashboardCssPath);
        assertTrue(
                studentCss.contains(".overview-card, .dashboard-panel") &&
                (studentCss.contains("border: 1.5px solid var(--ink)") || studentCss.contains("border: 1.5px solid #171714") || studentCss.contains("border:1.5px solid var(--ink)")),
                ".overview-card, .dashboard-panel must have crisp 1.5px ink border"
        );

        // student-packages.css
        Path studentPackagesCssPath = Path.of("src/main/resources/static/css/student-packages.css");
        assertTrue(Files.exists(studentPackagesCssPath), "student-packages.css must exist");
        String packagesCss = Files.readString(studentPackagesCssPath);
        assertTrue(
                packagesCss.contains(".package-card") &&
                (packagesCss.contains("border: 1.5px solid var(--ink)") || packagesCss.contains("border: 1.5px solid #171714") || packagesCss.contains("border:1.5px solid var(--ink)")),
                ".package-card must have crisp 1.5px ink border"
        );

        // teacher-dashboard.css
        Path teacherDashboardCssPath = Path.of("src/main/resources/static/css/teacher-dashboard.css");
        assertTrue(Files.exists(teacherDashboardCssPath), "teacher-dashboard.css must exist");
        String teacherCss = Files.readString(teacherDashboardCssPath);
        assertTrue(
                teacherCss.contains(".teacher-card-section {") &&
                (teacherCss.contains("border: 1.5px solid var(--ink)") || teacherCss.contains("border: 1.5px solid #171714") || teacherCss.contains("border:1.5px solid var(--ink)")),
                ".teacher-card-section must have crisp 1.5px ink border"
        );
    }
}
