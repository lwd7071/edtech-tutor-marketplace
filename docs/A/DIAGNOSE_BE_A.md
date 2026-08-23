# Diagnose Report - Codebase của Thành Viên A

Báo cáo phân tích tĩnh (Static Analysis) và kiểm thử (Test) trên toàn bộ các module do A thực hiện.

Mặc dù A làm được khối lượng code khổng lồ, nhưng code này đang **vi phạm nghiêm trọng 2 quy tắc cốt lõi** trong `PLANBE.md`.

## 1. Vi Phạm Kiến Trúc (Cross-module Repository Access)

> **Quy định trong PLANBE.md**: "Module chỉ giao tiếp qua public facade, DTO hoặc domain event; **không gọi repository của module khác**."

Thực tế, A đã inject chéo Repository để truy vấn dữ liệu thay vì gọi qua Facade/Service, phá vỡ nguyên tắc Độc lập Module (Decoupling). Danh sách các vi phạm:

- **Module `catalog`**
  - `PricingPackageService`: Gọi trực tiếp `SubjectRepository`, `TeacherProfileRepository`, `TeacherSubjectRepository`.
- **Module `common`**
  - `AttachmentService`: Gọi trực tiếp `UserRepository`.
- **Module `communication`**
  - `ChatService`: Gọi trực tiếp `AttachmentRepository`.
  - `NotificationEventListener`: Gọi trực tiếp `UserRepository`.
- **Module `learning`**
  - `StudentAssignmentService`: Gọi trực tiếp `UserRepository`, `AttachmentRepository`.
  - `TeacherAssignmentService`: Gọi trực tiếp `UserRepository`, `AttachmentRepository`, `SubjectRepository`, `TeacherProfileRepository`.
- **Module `ranking`**
  - `ReviewService`: Gọi trực tiếp `UserRepository`.
  - `TeacherStatsService`: Gọi trực tiếp `UserRepository`, `TeacherProfileRepository`.
- **Module `subject`**
  - `SubjectProposalService`: Gọi trực tiếp `TeacherProfileRepository`.
- **Module `teacher`**
  - `TeacherSubjectService`: Gọi trực tiếp `SubjectRepository`.

**Hậu quả**: Vi phạm nguyên tắc đóng gói của DDD. Nếu thay đổi DB Schema ở một module (vd: `User`), các module khác sẽ lỗi theo do Coupling quá chặt.

## 2. Thiếu Trầm Trọng Unit/Integration Test

> **Quy định trong PLANBE.md (Definition of Done)**: "Có unit test và integration test phù hợp rủi ro."

Khi chạy lệnh kiểm tra các file test (`mvn test`), kết quả cho thấy **gần như không có test nào** cho các module của A. A chỉ viết đúng một test duy nhất:
- `AuthRegisterTest.java` (thuộc module Auth).

Các module khổng lồ và chứa nhiều business logic cốt lõi như `catalog` (Marketplace), `learning`, `teacher`, `ranking` hoàn toàn không có bất kỳ dòng Unit Test nào.
Việc này rất nguy hiểm vì không có Feedback loop an toàn cho hệ thống.

---
**Kết Luận Diagnose & Hướng Giải Quyết:**
Trước khi phát triển tiếp, hệ thống CẦN BẮT BUỘC refactor code của A để:
1. Xóa bỏ toàn bộ các cross-module repository imports (Thay bằng việc tạo các Facade service trung gian hoặc Public Interface).
2. Viết Unit Test cho các business logic quan trọng nhất.
