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
    @DisplayName("public.css: .button--secondary phải có viền 1.5px tương phản rõ ràng")
    void secondaryButtonShouldHaveCrispBorder() throws Exception {
        Path publicCssPath = Path.of("src/main/resources/static/css/public.css");
        assertTrue(Files.exists(publicCssPath), "public.css must exist");
        String css = Files.readString(publicCssPath);

        assertTrue(
                css.contains(".button--secondary") && (css.contains("border: 1.5px solid") || css.contains("border-width: 1.5px")),
                ".button--secondary must have a crisp 1.5px border"
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
}
