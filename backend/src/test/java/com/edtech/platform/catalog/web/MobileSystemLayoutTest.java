package com.edtech.platform.catalog.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD: Kiểm tra hệ thống token và CSS utility cho nền tảng Mobile (UI/UX Pro Max).
 */
class MobileSystemLayoutTest {

    @Test
    @DisplayName("tokens.css phải định nghĩa đầy đủ token mobile: nav-height, touch-target và radius-sheet")
    void tokensCssShouldDefineMobileTokens() throws Exception {
        Path tokensPath = Path.of("src/main/resources/static/css/tokens.css");
        assertTrue(Files.exists(tokensPath), "tokens.css must exist");
        String css = Files.readString(tokensPath);

        assertTrue(
                css.contains("--mobile-nav-height: 64px;"),
                "tokens.css must define --mobile-nav-height: 64px;"
        );
        assertTrue(
                css.contains("--touch-target-min: 48px;"),
                "tokens.css must define --touch-target-min: 48px;"
        );
        assertTrue(
                css.contains("--radius-sheet: 16px;"),
                "tokens.css must define --radius-sheet: 16px;"
        );
    }

    @Test
    @DisplayName("public.css: .mobile-bottom-nav phải có viền mực 1.5px, fixed bottom và safe-area padding")
    void publicCssShouldDefineMobileBottomNavWithInkBorderAndSafeArea() throws Exception {
        Path publicCssPath = Path.of("src/main/resources/static/css/public.css");
        assertTrue(Files.exists(publicCssPath), "public.css must exist");
        String css = Files.readString(publicCssPath);

        assertTrue(
                css.contains(".mobile-bottom-nav"),
                "public.css must define .mobile-bottom-nav"
        );
        assertTrue(
                css.contains("border-top: 1.5px solid var(--ink)") || css.contains("border-top: 1.5px solid #171714"),
                ".mobile-bottom-nav must have crisp 1.5px ink top border"
        );
        assertTrue(
                css.contains("env(safe-area-inset-bottom"),
                ".mobile-bottom-nav must support safe-area-inset-bottom for mobile devices"
        );
    }

    @Test
    @DisplayName("public.css: .mobile-bottom-sheet phải có bo góc trên 16px và viền mực 1.5px")
    void publicCssShouldDefineMobileBottomSheet() throws Exception {
        Path publicCssPath = Path.of("src/main/resources/static/css/public.css");
        assertTrue(Files.exists(publicCssPath), "public.css must exist");
        String css = Files.readString(publicCssPath);

        assertTrue(
                css.contains(".mobile-bottom-sheet"),
                "public.css must define .mobile-bottom-sheet"
        );
        assertTrue(
                css.contains("border-radius: var(--radius-sheet) var(--radius-sheet) 0 0")
                        || css.contains("border-radius: 16px 16px 0 0"),
                ".mobile-bottom-sheet must use 16px top border radius"
        );
    }

    @Test
    @DisplayName("public.css: mobile inputs phải chống auto-zoom iOS (font-size >= 16px) và đạt chuẩn touch target")
    void publicCssShouldPreventIosInputAutoZoomAndEnforceTouchTargets() throws Exception {
        Path publicCssPath = Path.of("src/main/resources/static/css/public.css");
        assertTrue(Files.exists(publicCssPath), "public.css must exist");
        String css = Files.readString(publicCssPath);

        assertTrue(
                css.contains("font-size: 16px") && css.contains("/* Prevents iOS auto-zoom on focus */"),
                "public.css must enforce 16px font-size for mobile inputs to prevent iOS auto-zoom"
        );
        assertTrue(
                css.contains("min-height: var(--touch-target-min)"),
                "public.css must enforce --touch-target-min for mobile controls"
        );
    }
}
