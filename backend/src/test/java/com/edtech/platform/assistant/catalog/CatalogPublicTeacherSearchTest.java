package com.edtech.platform.assistant.catalog;

import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogPublicTeacherSearchTest {
    @Test
    void resolvesActiveSubjectAndUsesExistingPublicMarketplaceSearch() {
        UUID mathId = UUID.fromString("da8e1485-13ec-45c0-a6e7-3e78e2b458a8");
        AtomicReference<TeacherSearchParams> captured = new AtomicReference<>();
        TeacherMarketplaceService marketplace = new TeacherMarketplaceService(null, null, null, null, null, null) {
            @Override
            public Page<TeacherCard> searchTeachers(TeacherSearchParams params) {
                captured.set(params);
                return new PageImpl<>(List.of());
            }
        };
        SubjectFacade subjects = new SubjectFacade() {
            @Override public SubjectSnapshot getSubject(UUID id) { return null; }
            @Override public Map<UUID, SubjectSnapshot> getSubjects(Collection<UUID> ids) { return Map.of(); }
            @Override public List<SubjectSnapshot> getActiveSubjects() {
                return List.of(new SubjectSnapshot(mathId, "MATH", "Toán", "HIGH_SCHOOL", true));
            }
            @Override public boolean isSubjectActive(UUID id) { return mathId.equals(id); }
        };

        new CatalogPublicTeacherSearch(marketplace, subjects).findForQuestion("Tìm gia sư Toán");

        assertEquals(mathId, captured.get().subjectId());
        assertEquals("rating_desc", captured.get().sort());
        assertEquals(0, captured.get().page());
        assertEquals(3, captured.get().size());
    }
}
