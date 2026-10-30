package com.edtech.platform.assistant.catalog;

import com.edtech.platform.assistant.service.PublicTeacherSearch;
import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.subject.facade.SubjectFacade;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class CatalogPublicTeacherSearch implements PublicTeacherSearch {
    private final TeacherMarketplaceService marketplace;
    private final SubjectFacade subjectFacade;

    public CatalogPublicTeacherSearch(TeacherMarketplaceService marketplace, SubjectFacade subjectFacade) {
        this.marketplace = marketplace;
        this.subjectFacade = subjectFacade;
    }

    @Override
    public List<TeacherCard> findForQuestion(String question) {
        String normalizedQuestion = normalize(question);
        UUID subjectId = subjectFacade.getActiveSubjects().stream()
                .filter(subject -> normalizedQuestion.contains(normalize(subject.name())))
                .map(subject -> subject.id())
                .findFirst().orElse(null);
        TeacherSearchParams params = new TeacherSearchParams(null, subjectId, null, null, null,
                null, null, null, null, "rating_desc", null, null, 0, 3);
        Page<TeacherCard> results = marketplace.searchTeachers(params);
        return results.getContent();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
