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
    @DisplayName("public.css: .button--primary phải có viền mực 1.5px và hiệu ứng tactile shadow khi hover")
    void primaryButtonShouldHaveCrispInkBorderAndTactileShadow() throws Exception {
        Path publicCssPath = Path.of("src/main/resources/static/css/public.css");
        assertTrue(Files.exists(publicCssPath), "public.css must exist");
        String css = Files.readString(publicCssPath);

        assertTrue(
                css.contains("border: 1.5px solid var(--ink)") || css.contains("border: 1.5px solid #171714"),
                ".button--primary must have crisp 1.5px ink border"
        );
        assertTrue(
                css.contains("box-shadow: 0 2px 0 var(--ink)") || css.contains("box-shadow: 0 2px 0 #171714"),
                ".button--primary:hover must have tactile 2px hard shadow"
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
    @DisplayName("auth.css: nút submit form auth phải đồng bộ viền 1.5px ink và bóng nổi tactile")
    void authSubmitButtonShouldUseCrispInkBorder() throws Exception {
        Path authCssPath = Path.of("src/main/resources/static/css/auth.css");
        assertTrue(Files.exists(authCssPath), "auth.css must exist");
        String css = Files.readString(authCssPath);

        assertTrue(
                css.contains("border: 1.5px solid var(--ink)") || css.contains("border: 1.5px solid #171714"),
                ".auth-submit must have crisp 1.5px ink border"
        );
        assertTrue(
                css.contains("box-shadow: 0 2px 0 var(--ink)") || css.contains("box-shadow: 0 2px 0 #171714"),
                ".auth-submit:hover must have tactile 2px shadow"
        );
    }
}
