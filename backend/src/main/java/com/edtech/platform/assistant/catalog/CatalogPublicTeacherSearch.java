package com.edtech.platform.assistant.catalog;

import com.edtech.platform.assistant.service.PublicTeacherSearch;
import com.edtech.platform.assistant.service.PublicTeacherMatch;
import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CatalogPublicTeacherSearch implements PublicTeacherSearch {
    private static final Pattern GRADE_NUMBER = Pattern.compile("\\b(lop\\s*)?\\d{1,2}\\b");
    private static final Map<String, List<String>> SUBJECT_ALIASES = Map.of(
            "toan", List.of("toan", "math", "mathematics"),
            "ngu van", List.of("ngu van", "van", "literature"),
            "vat ly", List.of("vat ly", "ly", "physics"),
            "hoa hoc", List.of("hoa hoc", "hoa", "chemistry"),
            "tieng anh", List.of("tieng anh", "anh", "english"),
            "tin hoc", List.of("tin hoc", "computer science", "informatics"));
    private static final Set<String> ELEMENTARY_TERMS = Set.of("tieu hoc", "cap 1");
    private static final Set<String> MIDDLE_SCHOOL_TERMS = Set.of("thcs", "cap 2", "trung hoc co so");
    private static final Set<String> HIGH_SCHOOL_TERMS = Set.of("thpt", "cap 3", "trung hoc pho thong");
    private static final Set<String> UNIVERSITY_TERMS = Set.of("dai hoc", "university");

    private final TeacherMarketplaceService marketplace;
    private final SubjectFacade subjectFacade;

    public CatalogPublicTeacherSearch(TeacherMarketplaceService marketplace, SubjectFacade subjectFacade) {
        this.marketplace = marketplace;
        this.subjectFacade = subjectFacade;
    }

    @Override
    public List<PublicTeacherMatch> findForQuestion(String question) {
        String normalizedQuestion = normalize(question);
        List<SubjectSnapshot> activeSubjects = subjectFacade.getActiveSubjects().stream()
                .filter(SubjectSnapshot::isActive)
                .toList();
        String educationLevel = requestedEducationLevel(normalizedQuestion);
        List<SubjectSnapshot> matches = activeSubjects.stream()
                .filter(subject -> educationLevel == null || matchesEducationLevel(subject.educationLevel(), educationLevel))
                .filter(subject -> matchesSubject(normalizedQuestion, subject.name()))
                .toList();

        if (matches.isEmpty() && (educationLevel != null || containsKnownSubject(normalizedQuestion))) {
            return List.of();
        }

        Map<UUID, TeacherCard> distinctTeachers = new LinkedHashMap<>();
        List<SubjectSnapshot> subjectsToSearch = matches.isEmpty() ? java.util.Collections.singletonList(null) : matches;
        for (SubjectSnapshot subject : subjectsToSearch) {
            TeacherSearchParams params = new TeacherSearchParams(null, subject == null ? null : subject.id(),
                    null, null, null, null, null, null, null, "rating_desc", null, null, 0, 3);
            Page<TeacherCard> results = marketplace.searchTeachers(params);
            results.getContent().forEach(teacher -> distinctTeachers.putIfAbsent(teacher.id(), teacher));
        }

        Map<UUID, SubjectSnapshot> activeById = new java.util.HashMap<>();
        activeSubjects.forEach(subject -> activeById.put(subject.id(), subject));
        return distinctTeachers.values().stream()
                .sorted(Comparator.comparingDouble(TeacherCard::averageRating).reversed()
                        .thenComparing(Comparator.comparingInt(TeacherCard::yearsOfExperience).reversed())
                        .thenComparing(TeacherCard::id))
                .limit(3)
                .map(teacher -> toPublicMatch(teacher, activeById))
                .toList();
    }

    private static PublicTeacherMatch toPublicMatch(TeacherCard teacher, Map<UUID, SubjectSnapshot> subjectsById) {
        List<String> subjects = teacher.subjects() == null ? List.of() : teacher.subjects().stream()
                .map(subject -> subject.name()).distinct().toList();
        List<String> levels = teacher.subjects() == null ? List.of() : teacher.subjects().stream()
                .map(subject -> subjectsById.get(subject.id()))
                .filter(java.util.Objects::nonNull)
                .map(SubjectSnapshot::educationLevel)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        return new PublicTeacherMatch(teacher.fullName(), subjects, levels, teacher.yearsOfExperience(),
                teacher.startingPriceVnd(), teacher.averageRating(), "/teachers/" + teacher.id());
    }

    private static boolean matchesSubject(String question, String subjectName) {
        String baseName = GRADE_NUMBER.matcher(normalize(subjectName)).replaceAll(" ").trim();
        Set<String> questionWords = words(question);
        List<String> terms = new ArrayList<>();
        terms.add(baseName);
        SUBJECT_ALIASES.forEach((canonical, aliases) -> {
            if (baseName.contains(canonical)) terms.addAll(aliases);
        });
        return terms.stream().filter(term -> !term.isBlank()).anyMatch(term ->
                question.contains(term) || words(term).stream().allMatch(questionWords::contains));
    }

    private static boolean containsKnownSubject(String question) {
        return SUBJECT_ALIASES.values().stream().flatMap(List::stream).anyMatch(term ->
                question.contains(term) || words(term).stream().allMatch(words(question)::contains));
    }

    private static String requestedEducationLevel(String question) {
        if (HIGH_SCHOOL_TERMS.stream().anyMatch(question::contains)
                || List.of("lop 10", "lop 11", "lop 12", "khoi 10", "khoi 11", "khoi 12").stream().anyMatch(question::contains)) {
            return "HIGH_SCHOOL";
        }
        if (MIDDLE_SCHOOL_TERMS.stream().anyMatch(question::contains)
                || List.of("lop 6", "lop 7", "lop 8", "lop 9").stream().anyMatch(question::contains)) {
            return "MIDDLE_SCHOOL";
        }
        if (ELEMENTARY_TERMS.stream().anyMatch(question::contains)
                || List.of("lop 1", "lop 2", "lop 3", "lop 4", "lop 5").stream().anyMatch(question::contains)) {
            return "ELEMENTARY";
        }
        if (UNIVERSITY_TERMS.stream().anyMatch(question::contains)) return "UNIVERSITY";
        return null;
    }

    private static boolean matchesEducationLevel(String subjectLevel, String requestedLevel) {
        if (subjectLevel == null) return false;
        if ("ELEMENTARY".equals(requestedLevel)) {
            return "ELEMENTARY".equals(subjectLevel) || "PRIMARY_SCHOOL".equals(subjectLevel);
        }
        return requestedLevel.equals(subjectLevel);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static Set<String> words(String value) {
        return new HashSet<>(List.of(value.split("\\s+")));
    }
}
