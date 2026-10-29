package com.edtech.platform.catalog.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD Slice 1: Kiểm tra các tokens và CSS rules cho hệ thống Button Neubrutalism-lite (Preply style).
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

        // Nút chính phải có viền 1.5px dứt khoát
        assertTrue(
                css.contains("border: 1.5px solid var(--ink)") || css.contains("border: 1.5px solid #171714"),
                ".button--primary must have crisp 1.5px ink border"
        );

        // Khi hover phải có hiệu ứng bóng cứng tactile 2px
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
}
