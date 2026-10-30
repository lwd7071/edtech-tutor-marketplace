package com.edtech.platform.assistant.service;

import com.edtech.platform.assistant.dto.AssistantSource;

import java.util.List;
import java.util.Set;

/** Versioned, reviewed guidance. Paths are product routes, never model output. */
public final class AssistantPlaybook {
    public record Guide(String id, String title, String content, String path, Set<String> roles) {
        public AssistantSource source() { return new AssistantSource(title, path); }
    }

    private static final Set<String> ALL = Set.of("GUEST", "STUDENT", "TEACHER", "ADMIN");
    private static final Set<String> GUEST = Set.of("GUEST");
    private static final Set<String> STUDENT = Set.of("STUDENT");
    private static final Set<String> TEACHER = Set.of("TEACHER");
    private static final Set<String> ADMIN = Set.of("ADMIN");

    public static final List<Guide> GUIDES = List.of(
            new Guide("marketplace", "Tìm gia sư", "Tìm và lọc gia sư theo môn, hình thức, giá và thông tin công khai; mở hồ sơ để xem gói học và đánh giá.", "/teachers", ALL),
            new Guide("how_it_works", "Cách hoạt động", "Tutor Match kết nối học viên với gia sư cho học 1-1. Học viên tìm gia sư, học thử hoặc mua gói, rồi gửi yêu cầu đặt buổi. Gia sư xác nhận giờ học.", "/how-it-works", ALL),
            new Guide("register", "Tạo tài khoản", "Khách chọn đăng ký học viên hoặc gia sư, điền thông tin và xác minh email. Tác vụ cần tài khoản phải đăng nhập trước.", "/auth/register", GUEST),
            new Guide("login", "Đăng nhập", "Đăng nhập bằng tài khoản hoặc Google. Sau khi đăng nhập, hệ thống đưa bạn về không gian phù hợp với vai trò.", "/auth/login", GUEST),
            new Guide("password", "Quên mật khẩu", "Dùng trang Quên mật khẩu để yêu cầu email đặt lại mật khẩu; kiểm tra hộp thư và làm theo liên kết.", "/auth/forgot-password", GUEST),
            new Guide("student_trial", "Học thử", "Học viên gửi yêu cầu học thử từ hồ sơ gia sư và theo dõi phản hồi trong mục Yêu cầu. Yêu cầu chưa phải buổi học đã xác nhận.", "/student/requests", STUDENT),
            new Guide("student_package", "Gói học và thanh toán", "Học viên chọn gói đang bán trên hồ sơ gia sư và thanh toán hóa đơn. Gói đã mua và số lượt được theo dõi trong Gói học.", "/student/packages", STUDENT),
            new Guide("student_booking", "Đặt buổi học", "Sau khi mua gói, học viên gửi yêu cầu đặt buổi. Gia sư chốt thời gian và hình thức học; khi đó buổi học mới được xác nhận.", "/student/bookings", STUDENT),
            new Guide("student_learning", "Bài tập và buổi học", "Học viên xem lịch học, bài tập, bài nộp và phản hồi trong không gian học viên.", "/student/assignments", STUDENT),
            new Guide("student_support", "Yêu cầu hỗ trợ học viên", "Học viên theo dõi yêu cầu hoàn tiền hoặc gia hạn trong mục Yêu cầu; trao đổi với gia sư qua Tin nhắn. Bot không biết trạng thái yêu cầu cụ thể.", "/student/requests", STUDENT),
            new Guide("teacher_profile", "Hồ sơ gia sư", "Gia sư hoàn thiện hồ sơ, môn dạy và tài liệu xác minh, rồi gửi xét duyệt. Chỉ hồ sơ được duyệt mới có thể bán gói.", "/teacher/profile", TEACHER),
            new Guide("teacher_packages", "Gói dạy", "Gia sư đã được duyệt tạo và quản lý gói học trong không gian gia sư; gói đang bán xuất hiện ở marketplace.", "/teacher/packages", TEACHER),
            new Guide("teacher_schedule", "Lịch rảnh và lịch dạy", "Gia sư cập nhật lịch rảnh, phản hồi yêu cầu học thử và chốt giờ cho yêu cầu đặt buổi đã mua.", "/teacher/bookings", TEACHER),
            new Guide("teacher_learning", "Bài tập của gia sư", "Gia sư tạo và giao bài, xem bài nộp, chấm điểm và gửi phản hồi.", "/teacher/assignments", TEACHER),
            new Guide("teacher_payout", "Ví và rút tiền", "Gia sư xem ví, thiết lập tài khoản ngân hàng và tạo yêu cầu rút tiền trong không gian gia sư. Bot không biết số dư thực tế.", "/teacher/payouts", TEACHER),
            new Guide("admin_approval", "Duyệt gia sư", "Quản trị viên xem hàng đợi xét duyệt, kiểm tra hồ sơ và tài liệu theo quy trình trước khi ra quyết định.", "/admin/teachers", ADMIN),
            new Guide("admin_refunds", "Hoàn tiền", "Quản trị viên xem hàng đợi hoàn tiền, kiểm tra lý do và bằng chứng rồi ra quyết định trong trang Hoàn tiền.", "/admin/refunds", ADMIN),
            new Guide("admin_extensions", "Gia hạn", "Quản trị viên xem và xử lý các yêu cầu gia hạn gói học trong trang Gia hạn.", "/admin/extensions", ADMIN),
            new Guide("admin_payouts", "Rút tiền", "Quản trị viên kiểm tra yêu cầu rút tiền của gia sư và xử lý trong trang Rút tiền.", "/admin/payouts", ADMIN),
            new Guide("admin_settlements", "Quyết toán buổi học", "Quản trị viên xem hàng đợi quyết toán, kiểm tra xác nhận và báo cáo của hai bên trước khi quyết định tiền đang giữ.", "/admin/booking-settlements", ADMIN),
            new Guide("admin_subjects", "Môn học", "Quản trị viên xét đề xuất môn và quản lý danh mục môn học từ trang Môn học.", "/admin/subjects", ADMIN),
            new Guide("admin_settings", "Cài đặt và nhật ký", "Quản trị viên quản lý cấu hình nền tảng và xem nhật ký để kiểm tra các thay đổi vận hành.", "/admin/settings", ADMIN)
    );

    private AssistantPlaybook() {}

    public static List<Guide> forRole(String role) {
        return GUIDES.stream().filter(guide -> guide.roles().contains(role)).toList();
    }
}
