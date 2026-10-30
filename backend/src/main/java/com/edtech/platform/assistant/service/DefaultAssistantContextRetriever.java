package com.edtech.platform.assistant.service;

import com.edtech.platform.assistant.dto.AssistantSource;
import com.edtech.platform.catalog.dto.TeacherCard;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class DefaultAssistantContextRetriever implements AssistantContextRetriever {
    private static final String TEACHER_GUIDE = "Tìm gia sư tại /teachers. Học viên có thể lọc theo môn học, hình thức và mức giá; mở hồ sơ để xem kinh nghiệm, gói học và đánh giá.";
    private static final List<Guide> GUIDES = List.of(
            new Guide(List.of("tim gia su", "gia su", "tim thay", "tim co", "giao vien", "day kem"),
                    "Tìm gia sư", TEACHER_GUIDE, "/teachers"),
            new Guide(List.of("hoc thu", "dat lich", "goi hoc", "thanh toan", "buoi hoc", "yeu cau dat"),
                    "Cách hoạt động", "Học viên có thể gửi yêu cầu học thử để gia sư phản hồi, hoặc mua gói học và thanh toán hóa đơn. Sau khi mua gói, học viên gửi yêu cầu đặt buổi; gia sư xác nhận giờ học cụ thể. Yêu cầu học thử chưa phải buổi học đã xác nhận.", "/how-it-works"),
            new Guide(List.of("dang ky gia su", "tro thanh gia su", "ho so gia su", "duoc duyet", "tao goi", "lich ran"),
                    "Trở thành gia sư", "Gia sư chuẩn bị hồ sơ, giới thiệu môn giảng dạy và kinh nghiệm, gửi hồ sơ để xét duyệt. Sau khi được duyệt, gia sư có thể tạo gói học, cập nhật lịch rảnh và phản hồi yêu cầu học thử.", "/become-a-tutor"),
            new Guide(List.of("hoan tien", "gia han", "tin nhan", "thong bao", "bai tap", "bao cao buoi hoc"),
                    "Trung tâm hỗ trợ", "Học viên theo dõi yêu cầu học thử, hoàn tiền hoặc gia hạn tại không gian học viên; tin nhắn, bài tập và báo cáo buổi học có khu vực riêng sau khi đăng nhập.", "/support"),
            new Guide(List.of("xin chao", "chao ban", "hello", "hi"),
                    "Cách hoạt động", "Chào bạn! Mình có thể hướng dẫn tìm gia sư, học thử, đặt lịch hoặc đăng ký làm gia sư trên Tutor Match.", "/how-it-works")
    );

    private final PublicTeacherSearch teacherSearch;

    public DefaultAssistantContextRetriever(PublicTeacherSearch teacherSearch) {
        this.teacherSearch = teacherSearch;
    }

    @Override
    public Optional<AssistantContext> retrieve(String question) {
        String normalizedQuestion = normalize(question);
        List<String> evidence = new ArrayList<>();
        List<AssistantSource> sources = new ArrayList<>();
        for (Guide guide : GUIDES) {
            if (guide.keywords().stream().anyMatch(normalizedQuestion::contains)) {
                evidence.add(guide.evidence());
                addSource(sources, new AssistantSource(guide.label(), guide.url()));
            }
        }

        if (isTutorQuestion(normalizedQuestion)) {
            List<TeacherCard> teachers = teacherSearch.findForQuestion(question);
            for (TeacherCard teacher : teachers) {
                String subjects = teacher.subjects() == null ? "" : teacher.subjects().stream()
                        .map(subject -> subject.name()).reduce((left, right) -> left + ", " + right).orElse("");
                evidence.add("Gia sư công khai: " + teacher.fullName() + "; môn: " + subjects
                        + "; kinh nghiệm: " + teacher.yearsOfExperience() + " năm; giá khởi điểm: "
                        + teacher.startingPriceVnd() + " VND; đánh giá trung bình: " + teacher.averageRating() + ".");
                addSource(sources, new AssistantSource(teacher.fullName(), "/teachers/" + teacher.id()));
            }
        }

        if (evidence.isEmpty()) return Optional.empty();
        return Optional.of(new AssistantContext(String.join("\n", evidence), sources));
    }

    private static boolean isTutorQuestion(String question) {
        return List.of("gia su", "giao vien", "tim thay", "tim co", "day kem", "tim nguoi day")
                .stream().anyMatch(question::contains);
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static void addSource(List<AssistantSource> sources, AssistantSource source) {
        if (!sources.contains(source)) sources.add(source);
    }

    private record Guide(List<String> keywords, String label, String evidence, String url) {
    }
}
