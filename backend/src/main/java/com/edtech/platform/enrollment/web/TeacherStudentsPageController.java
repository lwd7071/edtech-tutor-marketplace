package com.edtech.platform.enrollment.web;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.enrollment.dto.StudentPackageDetail;
import com.edtech.platform.enrollment.service.TeacherLearnerService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TeacherStudentsPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherStudentsPageController.class);
    private static final int PAGE_SIZE = 12;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
            .withZone(VIETNAM_ZONE);

    private final TeacherLearnerService learners;

    @GetMapping("/teacher/students")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "0") int page, Model model, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        model.addAttribute("activeWorkspaceNav", "students");
        int safePage = Math.max(0, page);
        try {
            Page<TeacherLearnerService.LearnerPackage> result = learners.list(user.id(), null,
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty())
                return "redirect:/teacher/students?page=" + Math.max(0, result.getTotalPages() - 1);
            model.addAttribute("studentRows", groupRows(result.getContent()));
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("totalPackages", result.getTotalElements());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
            model.addAttribute("studentsUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher student list is unavailable");
            model.addAttribute("studentRows", List.of());
            model.addAttribute("studentsUnavailable", true);
        }
        return "workspace/teacher-students";
    }

    @GetMapping("/teacher/students/{studentId}")
    public String detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID studentId,
                         @RequestParam(defaultValue = "0") int page, Model model, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        int safePage = Math.max(0, page);
        model.addAttribute("activeWorkspaceNav", "students");
        model.addAttribute("studentId", studentId);
        model.addAttribute("studentName", "Học viên");
        model.addAttribute("studentNotFound", false);
        try {
            Page<TeacherLearnerService.LearnerPackage> result = learners.list(user.id(), studentId,
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
            if (result.getTotalElements() == 0) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                model.addAttribute("studentNotFound", true);
                return "workspace/teacher-student-detail";
            }
            if (safePage > 0 && result.isEmpty())
                return "redirect:/teacher/students/" + studentId + "?page=" + Math.max(0, result.getTotalPages() - 1);
            String studentName = result.getContent().isEmpty() ? "Học viên" : safeName(result.getContent().get(0).studentName());
            model.addAttribute("studentName", studentName);
            model.addAttribute("packageRows", result.getContent().stream().map(row -> toPackageRow(row.studentPackage(), row.subjectName())).toList());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("totalPackages", result.getTotalElements());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
            model.addAttribute("studentUnavailable", false);
        } catch (RuntimeException ex) {
            log.warn("Teacher student detail is unavailable");
            model.addAttribute("studentUnavailable", true);
        }
        return "workspace/teacher-student-detail";
    }

    private List<StudentRow> groupRows(List<TeacherLearnerService.LearnerPackage> rows) {
        var grouped = new LinkedHashMap<UUID, StudentRowBuilder>();
        for (var row : rows) {
            StudentRowBuilder builder = grouped.computeIfAbsent(row.studentId(), id -> new StudentRowBuilder(id, safeName(row.studentName())));
            builder.packages.add(toPackageRow(row.studentPackage(), row.subjectName()));
        }
        return grouped.values().stream().map(builder -> new StudentRow(builder.studentId, builder.name, initial(builder.name), List.copyOf(builder.packages))).toList();
    }

    private PackageRow toPackageRow(StudentPackageDetail pkg, String subjectName) {
        String expiry = pkg.expiresAt() == null ? "Không giới hạn" : DATE_FORMAT.format(pkg.expiresAt());
        return new PackageRow(pkg.packageName(), safeName(subjectName), pkg.status(), statusLabel(pkg.status()), pkg.totalSessions(),
                pkg.remainingSessions(), pkg.reservedSessions(), pkg.completedSessions(), pkg.refundedSessions(),
                NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(pkg.purchasePriceVnd()) + " ₫", expiry);
    }

    private String statusLabel(String raw) {
        return switch (raw) {
            case "ACTIVE" -> "Đang hoạt động";
            case "PENDING_PAYMENT" -> "Chờ thanh toán";
            case "COMPLETED" -> "Đã hoàn thành";
            case "REFUND_PENDING" -> "Đang xử lý hoàn tiền";
            case "REFUNDED" -> "Đã hoàn tiền";
            case "LOCKED_EXPIRED" -> "Đã hết hạn";
            default -> "Đang cập nhật";
        };
    }

    private String safeName(String value) { return value == null || value.isBlank() ? "Đang cập nhật" : value; }
    private String initial(String value) { return value.isBlank() ? "H" : new String(Character.toChars(value.codePointAt(0))); }
    private void noStore(HttpServletResponse response) { response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store"); }
    private void requireTeacher(AuthenticatedUser user) {
        if (user == null || !"TEACHER".equals(user.role())) throw new AccessDeniedException("Teacher workspace only");
    }

    private static final class StudentRowBuilder {
        private final UUID studentId;
        private final String name;
        private final List<PackageRow> packages = new ArrayList<>();
        private StudentRowBuilder(UUID studentId, String name) { this.studentId = studentId; this.name = name; }
    }
    public record StudentRow(UUID studentId, String studentName, String initial, List<PackageRow> packages) {}
    public record PackageRow(String packageName, String subjectName, String statusCode, String statusLabel, int totalSessions,
                             int remainingSessions, int reservedSessions, int completedSessions, int refundedSessions,
                             String purchasePriceLabel, String expiryLabel) {}
}
