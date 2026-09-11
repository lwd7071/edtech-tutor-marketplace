package com.edtech.platform.catalog.service;

import com.edtech.platform.catalog.dto.TeacherCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

public record CachedTeacherSearchPage(
        List<TeacherCard> content,
        int page,
        int size,
        long totalElements
) {
    public CachedTeacherSearchPage {
        content = new ArrayList<>(content);
    }

    public static CachedTeacherSearchPage from(Page<TeacherCard> source) {
        return new CachedTeacherSearchPage(
                source.getContent(), source.getNumber(), source.getSize(), source.getTotalElements());
    }

    public Page<TeacherCard> toPage() {
        return new PageImpl<>(content, PageRequest.of(page, size), totalElements);
    }
}
