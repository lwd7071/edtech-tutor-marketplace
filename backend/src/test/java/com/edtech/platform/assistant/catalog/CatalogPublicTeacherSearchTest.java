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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CatalogPublicTeacherSearchTest {
    @Test
    void mapsHighSchoolLanguageToHighSchoolSubjectWhenNamesOverlap() {
        UUID middleSchoolMathId = UUID.fromString("da8e1485-13ec-45c0-a6e7-3e78e2b458a8");
        UUID highSchoolMathId = UUID.fromString("ecb30286-72e4-4423-b69e-0bdb98a30f2e");
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
                return List.of(
                        new SubjectSnapshot(middleSchoolMathId, "MATH_MS", "Toán", "MIDDLE_SCHOOL", true),
                        new SubjectSnapshot(highSchoolMathId, "MATH_HS", "Toán", "HIGH_SCHOOL", true));
            }
            @Override public boolean isSubjectActive(UUID id) { return true; }
        };

        new CatalogPublicTeacherSearch(marketplace, subjects).findForQuestion("Có gia sư dạy Toán cấp 3 không?");

        assertEquals(highSchoolMathId, captured.get().subjectId());
    }

    @Test
    void publicAssistantMatchContainsOnlyAllowlistedTeacherFields() {
        UUID teacherId = UUID.fromString("0e7e3202-5968-402b-9716-cfcb343e8040");
        UUID mathId = UUID.fromString("f234395d-b49d-4896-b45d-8165dad8576f");
        TeacherCard card = new TeacherCard(teacherId, "Mai Nguyễn", "PRIVATE_AVATAR_SENTINEL",
                "PRIVATE_BIO_SENTINEL", 5, true, true, false,
                List.of(new com.edtech.platform.catalog.dto.SubjectDto(mathId, "Toán")),
                250000, 4.8, 4.7, 12, 3, "PRIVATE_PROVINCE_SENTINEL", "PRIVATE_WARD_SENTINEL");
        TeacherMarketplaceService marketplace = new TeacherMarketplaceService(null, null, null, null, null, null) {
            @Override public Page<TeacherCard> searchTeachers(TeacherSearchParams params) {
                return new PageImpl<>(List.of(card));
            }
        };
        SubjectFacade subjects = new SubjectFacade() {
            @Override public SubjectSnapshot getSubject(UUID id) { return null; }
            @Override public Map<UUID, SubjectSnapshot> getSubjects(Collection<UUID> ids) { return Map.of(); }
            @Override public List<SubjectSnapshot> getActiveSubjects() {
                return List.of(new SubjectSnapshot(mathId, "MATH", "Toán", "HIGH_SCHOOL", true));
            }
            @Override public boolean isSubjectActive(UUID id) { return true; }
        };

        var match = new CatalogPublicTeacherSearch(marketplace, subjects)
                .findForQuestion("Tìm gia sư Toán cấp 3")
                .getFirst();

        assertEquals("Mai Nguyễn", match.fullName());
        assertEquals(List.of("Toán"), match.subjects());
        assertEquals(List.of("HIGH_SCHOOL"), match.educationLevels());
        assertEquals("/teachers/" + teacherId, match.profilePath());
        assertFalse(match.toString().contains("PRIVATE_"));
    }

    @Test
    void unavailableRequestedSubjectDoesNotFallBackToUnfilteredTeacherSearch() {
        AtomicInteger calls = new AtomicInteger();
        TeacherMarketplaceService marketplace = new TeacherMarketplaceService(null, null, null, null, null, null) {
            @Override public Page<TeacherCard> searchTeachers(TeacherSearchParams params) {
                calls.incrementAndGet();
                return new PageImpl<>(List.of());
            }
        };
        SubjectFacade subjects = new SubjectFacade() {
            @Override public SubjectSnapshot getSubject(UUID id) { return null; }
            @Override public Map<UUID, SubjectSnapshot> getSubjects(Collection<UUID> ids) { return Map.of(); }
            @Override public List<SubjectSnapshot> getActiveSubjects() {
                return List.of(new SubjectSnapshot(UUID.randomUUID(), "LIT", "Ngữ văn", "HIGH_SCHOOL", true));
            }
            @Override public boolean isSubjectActive(UUID id) { return true; }
        };

        assertEquals(List.of(), new CatalogPublicTeacherSearch(marketplace, subjects)
                .findForQuestion("Tìm gia sư Toán"));
        assertEquals(0, calls.get());
    }

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
