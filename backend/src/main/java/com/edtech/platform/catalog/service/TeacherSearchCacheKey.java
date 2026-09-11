package com.edtech.platform.catalog.service;

import com.edtech.platform.catalog.dto.TeacherSearchParams;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TeacherSearchCacheKey {
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    private TeacherSearchCacheKey() {
    }

    public static String from(TeacherSearchParams params) {
        String slot = params.dayOfWeek() == null
                ? "none"
                : value(params.dayOfWeek()) + "," + value(params.startTime()) + "," + value(params.endTime());
        return "teacher-search:v1"
                + ":keyword=" + normalizeKeyword(params.keyword())
                + ":subject=" + value(params.subjectId())
                + ":price=" + value(params.minPrice()) + "-" + value(params.maxPrice())
                + ":rating=" + value(params.minRating())
                + ":mode=" + value(params.deliveryMode()).toLowerCase(Locale.ROOT)
                + ":slot=" + slot.toLowerCase(Locale.ROOT)
                + ":page=" + page(params)
                + ":size=" + size(params)
                + ":sort=" + value(params.sort()).toLowerCase(Locale.ROOT);
    }

    public static boolean isCacheable(TeacherSearchParams params) {
        return page(params) <= 2 && size(params) <= 50;
    }

    static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "all";
        }
        String decomposed = Normalizer.normalize(keyword.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return COMBINING_MARKS.matcher(decomposed).replaceAll("").replace('đ', 'd');
    }

    private static int page(TeacherSearchParams params) {
        return params.page() == null ? 0 : params.page();
    }

    private static int size(TeacherSearchParams params) {
        return params.size() == null ? 20 : params.size();
    }

    private static String value(Object value) {
        return value == null ? "all" : value.toString();
    }
}
