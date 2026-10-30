package com.edtech.platform.assistant.service;

import com.edtech.platform.assistant.dto.AssistantSource;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class DefaultAssistantContextRetriever implements AssistantContextRetriever {
    private static final String NO_TUTOR_RESULTS = "Mình chưa tìm thấy gia sư phù hợp trong danh sách công khai hiện tại. Bạn có thể đổi môn hoặc cấp học tại trang Tìm gia sư.";
    private static final Set<String> STOP_WORDS = Set.of("la", "gi", "cua", "cho", "the", "nao", "tren", "va", "toi", "minh", "ban", "hay", "ve", "nhu", "trong", "de", "tai", "sau", "co", "khong", "muon", "can", "duoc", "web", "website");
    private static final List<String> TUTOR_SEARCH_TERMS = List.of(
            "tim gia su", "tim giao vien", "tim nguoi day", "gia su nao", "giao vien nao",
            "co gia su", "co giao vien", "gia su day", "giao vien day", "day kem", "tim thay", "ai day");

    private final PublicTeacherSearch teacherSearch;
    private final AssistantKnowledgeSearch knowledgeSearch;

    public DefaultAssistantContextRetriever(PublicTeacherSearch teacherSearch, AssistantKnowledgeSearch knowledgeSearch) {
        this.teacherSearch = teacherSearch;
        this.knowledgeSearch = knowledgeSearch;
    }

    @Override
    public Optional<AssistantContext> retrieve(String question) {
        String normalizedQuestion = normalize(question);
        List<String> evidence = new ArrayList<>();
        List<AssistantSource> sources = new ArrayList<>();
        List<String> questionTokens = meaningfulTokens(normalizedQuestion);
        List<AssistantKnowledgeArticle> articles = knowledgeSearch.findPublished().stream()
                .filter(article -> matchesArticle(article, normalizedQuestion, questionTokens))
                .limit(3)
                .toList();
        for (AssistantKnowledgeArticle article : articles) {
            evidence.add(article.title() + ": " + article.content());
            addSource(sources, new AssistantSource(article.title(), article.sourcePath()));
        }

        if (isTutorSearchQuestion(normalizedQuestion)) {
            List<PublicTeacherMatch> teachers = teacherSearch.findForQuestion(question);
            if (teachers.isEmpty()) {
                addSource(sources, new AssistantSource("Tìm gia sư", "/teachers"));
                return Optional.of(new AssistantContext("", sources, NO_TUTOR_RESULTS));
            }
            for (PublicTeacherMatch teacher : teachers) {
                String subjects = String.join(", ", teacher.subjects());
                String levels = teacher.educationLevels().isEmpty() ? "" : "; cấp học: "
                        + String.join(", ", teacher.educationLevels());
                evidence.add("Gia sư công khai: " + teacher.fullName() + "; môn: " + subjects + levels
                        + "; kinh nghiệm: " + teacher.yearsOfExperience() + " năm; giá khởi điểm: "
                        + teacher.startingPriceVnd() + " VND; đánh giá trung bình: " + teacher.averageRating() + ".");
                addSource(sources, new AssistantSource(teacher.fullName(), teacher.profilePath()));
            }
        }

        if (evidence.isEmpty()) return Optional.empty();
        return Optional.of(new AssistantContext(String.join("\n", evidence), sources));
    }

    private static boolean matchesArticle(AssistantKnowledgeArticle article, String question, List<String> questionTokens) {
        List<String> candidates = new ArrayList<>(article.aliases());
        candidates.add(article.title());
        return candidates.stream().map(DefaultAssistantContextRetriever::normalize).anyMatch(alias -> {
            if (alias.isBlank()) return false;
            if (question.contains(alias)) return true;
            List<String> aliasTokens = meaningfulTokens(alias);
            return aliasTokens.size() >= 2 && questionTokens.containsAll(aliasTokens);
        });
    }

    private static boolean isTutorSearchQuestion(String question) {
        return TUTOR_SEARCH_TERMS.stream().anyMatch(question::contains);
    }

    private static List<String> meaningfulTokens(String value) {
        return List.of(value.split("\\s+" )).stream()
                .filter(token -> token.length() >= 2 && !STOP_WORDS.contains(token))
                .distinct()
                .toList();
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static void addSource(List<AssistantSource> sources, AssistantSource source) {
        if (!sources.contains(source)) sources.add(source);
    }

}
