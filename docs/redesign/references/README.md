# Visual reference cho tutormatch

## Quyền sử dụng và trạng thái

Concept nền **Bàn học mở** đã được duyệt toàn website; các quyết định còn hiệu lực nằm trong ADR 0015 và `../design/DESIGN_SYSTEM.md`. Reference P (2026-10-15), A/S/T/X (2026-10-16) đã được duyệt. Đây là ảnh concept từ imagegen, không phải screenshot app hay bằng chứng test. Chỉ dùng phần bố cục được duyệt, không sao chép text/dữ liệu/chức năng tạo sinh.

| Mã | Nhóm | Ảnh đề xuất | Cần duyệt phần nào |
|---|---|---|---|
| P | Public | [public-proposal.png](public-proposal.png) — đã duyệt | Nhịp trang chủ, chọn môn, list + preview và bản mobile |
| A | Auth | [auth-proposal.png](auth-proposal.png) — đã duyệt 2026-10-16 | Form tập trung, vị trí trợ giúp/lỗi, desktop/mobile |
| S | Student | [student-proposal.png](student-proposal.png) — đã duyệt 2026-10-16 | Việc học tiếp theo, gói/bài tập, điều hướng mobile |
| T | Teacher | [teacher-proposal.png](teacher-proposal.png) — đã duyệt 2026-10-16 | Trạng thái hồ sơ, hàng đợi học thử, lịch và quản lý gói |
| X | Admin | [admin-proposal.png](admin-proposal.png) — đã duyệt 2026-10-16 | Queue, filter, context drawer và decision trên desktop/mobile |

Các ảnh concept và ảnh chụp đối chiếu từ đợt trước đã được dọn khỏi `design/` vì không thuộc bộ đặc tả này. Quyết định thương hiệu đã duyệt vẫn theo ADR 0015 và `DESIGN_SYSTEM.md`; năm ảnh đề xuất bên dưới là reference duy nhất của bộ tài liệu hiện tại.

Nguồn tham khảo bên ngoài được đọc ngày 2026-10-15: [Preply homepage](https://preply.com/) và [tutor discovery](https://preply.com/en/online/english-tutors). Mượn nguyên tắc: tiêu đề và CTA rõ, khám phá gia sư theo mục tiêu, filter có thứ tự, thông tin giúp so sánh và bước tiếp theo gần ngữ cảnh. Không mượn nhãn thương hiệu, dữ liệu, chính sách, số liệu, giá, rating hoặc chức năng chưa có tại TutorMatch.

## Điều bắt buộc bỏ qua trong ảnh tạo sinh

- Mọi tên người, ảnh chân dung, ngày, số lượng, mức giá, rating, môn, cấp học, review, lời hứa và bản dịch tiếng Anh trong ảnh là placeholder. Một số ảnh vẽ thêm action, trạng thái, thống kê hoặc đường dẫn không có trong repo; không triển khai chúng.
- Nếu ảnh có wordmark kèm dấu tròn, dấu chấm, hoặc chữ không đúng `tutormatch`, dùng quy chuẩn trong `DESIGN_SYSTEM.md` thay thế. Mọi mũi tên trang trí trong ảnh cũng bị loại bỏ.
- Auth image có các field/vị trí Google chỉ minh họa; source form và `PAGE_DESIGN_PLAN.md` quyết định thứ tự thật. Admin image có trạng thái queue minh họa; chỉ dùng trạng thái API hiện có.
- Khi ảnh và code/API không khớp, ghi chú khác biệt ở page spec và giữ contract. Reference duyệt khóa **bố cục**, không cấp quyền thêm tính năng.

## Prompt đã dùng

- **P:** concept board public desktop và mobile: home hero, subject selection, vertical tutor results + contextual preview; tham khảo hierarchy Preply, màu vàng/mực và wordmark chữ thường; không dữ liệu giả khi triển khai.
- **A:** board login/register desktop và login mobile: form gọn, validation/processing/recovery, palette TutorMatch, text-first actions.
- **S:** board student dashboard/mobile/chi tiết gói và bài: upcoming booking, remaining sessions, assignments, messages, task-first hierarchy.
- **T:** board teacher overview/mobile trial requests/profile approval/availability/packages: việc cần làm và trạng thái duyệt trước.
- **X:** board admin operations queue/mobile/detail drawer: filter, status, bằng chứng trước quyết định.

Các prompt dùng công cụ imagegen tích hợp. Ảnh được copy từ vùng generated images của Codex vào `docs/redesign/references/`; không dùng API key hoặc dịch vụ ảnh ngoài. Người duyệt phản hồi theo mã P/A/S/T/X; cả năm reference đã được duyệt (A/S/T/X ngày 2026-10-16).
