# Đặc tả Frontend — Tutor Match

Đây là nguồn chuẩn cho cấu trúc giao diện, trang và phân quyền. Request/response nằm trong [`API_CONTRACT.md`](API_CONTRACT.md); hướng dẫn chạy nằm trong [`frontend/README.md`](../../frontend/README.md).

## Nguyên tắc sản phẩm

- Luồng chính: tìm gia sư → xem hồ sơ → chọn gói hoặc học thử → thanh toán → học → làm bài → đánh giá.
- Thẻ gia sư chỉ hiện ảnh, họ tên, môn tiêu biểu, kinh nghiệm, rating, hình thức dạy và giá gói thấp nhất. Thông tin đầy đủ nằm ở trang chi tiết.
- Danh sách dùng bộ lọc cần thiết và phân trang từ server.
- Mọi màn hình dữ liệu có loading, lỗi kèm thử lại, trạng thái trống và trạng thái thành công.
- FE lấy quyền và trạng thái nghiệp vụ từ Backend; không dựng dữ liệu giả khi API lỗi hoặc thiếu trường.
- Public và Student ưu tiên mobile. Teacher và Admin ưu tiên desktop nhưng vẫn responsive.

## Ngôn ngữ và định dạng

- Thuật ngữ chuẩn: **gia sư**, **học viên**, **quản trị viên**, **gói học**, **buổi học**, **học thử**.
- Tiền: VND số nguyên, ví dụ `1.000.000đ`.
- Thời gian: API trả ISO-8601; FE hiển thị bằng locale `vi-VN` theo múi giờ thiết bị.
- Không hiện UUID đầy đủ cho người dùng. Khi cần đối soát, dùng mã nghiệp vụ hoặc tám ký tự đầu.
- `referenceUrl` từ Backend chỉ được điều hướng khi là đường dẫn nội bộ bắt đầu bằng `/`.

## Khung giao diện theo vai trò

### Public

Navbar gồm logo, tìm gia sư, môn học, cách hoạt động, trở thành gia sư và tài khoản. Homepage gồm hero tìm kiếm, môn nổi bật, gia sư nổi bật, cách hoạt động và CTA. Footer chứa liên kết sản phẩm và hỗ trợ.

### Student

Menu: Tổng quan, Gói học, Lịch học, Bài tập, Tin nhắn, Thông báo, Yêu cầu và Hồ sơ. Mobile dùng nội dung một cột và điều hướng gọn.

### Teacher

Menu: Tổng quan, Hồ sơ, Tài liệu, Môn dạy, Đề xuất môn, Gói học, Lịch rảnh, Học viên, Lịch dạy, Học thử, Bài tập, Tin nhắn, Thông báo, Thống kê, Ví, Ngân hàng và Rút tiền.

Các trạng thái `DRAFT`, `PENDING_APPROVAL`, `REJECTED` phải có hướng dẫn rõ. Nghiệp vụ yêu cầu hồ sơ đã duyệt dùng `TeacherApprovalGuard`.

### Admin

Menu: Dashboard, Duyệt gia sư, Đề xuất môn, Hoàn tiền, Gia hạn, Rút tiền, Cài đặt và Nhật ký. Các trang danh sách ưu tiên filter, phân trang, trạng thái và thao tác trong drawer/modal.

## Danh sách trang

### Public và xác thực

| Route | Nội dung |
|---|---|
| `/` | Hero, môn nổi bật, gia sư nổi bật, cách hoạt động và CTA |
| `/teachers` | Lọc môn/hình thức/giá/rating/lịch rảnh, sắp xếp, thẻ và phân trang |
| `/teachers/[id]` | Hồ sơ, môn, gói học, lịch rảnh, đánh giá và CTA học thử |
| `/subjects` | Danh mục môn; chọn môn dẫn sang danh sách gia sư đã lọc |
| `/ranking` | Bảng xếp hạng từ dữ liệu Backend |
| `/how-it-works` | Quy trình dành cho học viên và gia sư |
| `/become-a-tutor` | Lợi ích, điều kiện và CTA đăng ký gia sư |
| `/auth/login` | Đăng nhập và Google OAuth |
| `/auth/register` | Đăng ký Student/Teacher; hỗ trợ `?role=TEACHER` |
| `/auth/verify-email` | Xác minh và gửi lại email |
| `/auth/forgot-password`, `/auth/reset-password` | Khôi phục mật khẩu |
| `/auth/oauth/role` | Chọn vai trò sau OAuth |
| `/forbidden` | Thông báo không có quyền |

### Student

| Route | Nội dung |
|---|---|
| `/student` | Tổng quan gói, buổi sắp tới và lối tắt |
| `/student/packages`, `/student/packages/[id]` | Danh sách và chi tiết gói đã mua |
| `/student/checkout/[invoiceId]` | QR/link thanh toán và polling Invoice |
| `/student/payment-result/[invoiceId]`, `/student/payments/callback` | Kết quả và callback thanh toán |
| `/student/bookings`, `/student/bookings/[id]` | Lịch và chi tiết buổi học |
| `/student/assignments`, `/student/assignments/[id]` | Danh sách, đề bài, bài nộp, điểm và phản hồi |
| `/student/messages` | Hội thoại và chat realtime |
| `/student/notifications` | Thông báo, đọc từng mục hoặc đọc tất cả |
| `/student/requests` | Yêu cầu hoàn tiền và gia hạn |
| `/student/profile` | Hồ sơ và thông tin liên hệ |

### Teacher

| Route | Nội dung |
|---|---|
| `/teacher` | Tổng quan hồ sơ, công việc và buổi sắp tới |
| `/teacher/profile`, `/teacher/documents` | Hồ sơ và tài liệu xác minh |
| `/teacher/subjects`, `/teacher/subject-proposals` | Môn dạy và đề xuất môn |
| `/teacher/packages`, `/teacher/packages/create`, `/teacher/packages/[id]` | Danh sách, tạo và chỉnh sửa gói |
| `/teacher/availability` | Lịch rảnh theo tuần; khóa lưu khi tải lịch lỗi |
| `/teacher/students`, `/teacher/students/[id]` | Học viên và gói đang học |
| `/teacher/bookings`, `/teacher/bookings/[id]` | Lịch dạy và chi tiết buổi học |
| `/teacher/trial-requests` | Chấp nhận/từ chối học thử và tạo buổi |
| `/teacher/assignments`, `/teacher/assignments/new`, `/teacher/assignments/[id]` | Quản lý và giao bài |
| `/teacher/submissions/[id]` | Chấm điểm và phản hồi |
| `/teacher/messages`, `/teacher/notifications` | Chat và thông báo |
| `/teacher/stats` | Số liệu thật về học viên, buổi học và đánh giá |
| `/teacher/wallet`, `/teacher/bank-accounts`, `/teacher/payouts` | Ví, ngân hàng và rút tiền |

### Admin

| Route | Nội dung |
|---|---|
| `/admin`, `/admin/dashboard` | Tổng quan và các hàng đợi cần xử lý |
| `/admin/teachers` | Duyệt hồ sơ và tài liệu gia sư |
| `/admin/subjects` | Duyệt đề xuất môn mới hoặc gắn môn hiện có |
| `/admin/refunds`, `/admin/extensions`, `/admin/payouts` | Xử lý các yêu cầu tài chính |
| `/admin/settings` | Cấu hình nền tảng |
| `/admin/audit-logs` | Nhật ký quản trị |

## Ma trận quyền

| Khả năng | Guest | Student | Teacher | Admin |
|---|:---:|:---:|:---:|:---:|
| Xem marketplace | ✓ | ✓ | ✓ | ✓ |
| Mua gói, học và nộp bài |  | ✓ |  |  |
| Yêu cầu hoàn tiền/gia hạn |  | ✓ |  |  |
| Quản lý hồ sơ, lịch, gói và bài tập |  |  | ✓ |  |
| Chat trong tài khoản |  | ✓ | ✓ |  |
| Ví và yêu cầu rút tiền |  |  | ✓ |  |
| Duyệt và cấu hình hệ thống |  |  |  | ✓ |

Guest bấm hành động cần tài khoản được đưa tới đăng nhập kèm URL quay lại. Người sai vai trò không thấy CTA; truy cập URL trực tiếp được guard chuyển sang `/forbidden`.

## Quy tắc trạng thái

1. **Loading:** skeleton hoặc spinner theo hình dạng nội dung.
2. **Error:** câu ngắn, không lộ lỗi kỹ thuật, có `Thử lại` khi có thể lặp.
3. **Empty:** giải thích chưa có dữ liệu và CTA hợp lệ theo quyền.
4. **Success:** chỉ cập nhật sau response thành công; mutation quan trọng chống submit lặp.

Chat phải khử trùng optimistic message với echo WebSocket. Notification chỉ đánh dấu đã đọc sau khi API thành công. Checkout chỉ tin trạng thái Invoice từ Backend, không dùng query callback làm bằng chứng thanh toán.

## Responsive và khả năng sử dụng

- Mobile: một cột, CTA dễ chạm, filter thu gọn; bảng/lịch rộng cuộn ngang có chủ đích.
- Tablet: danh sách thẻ hai cột khi đủ chỗ.
- Desktop: marketplace có sidebar filter; dashboard có sidebar điều hướng.
- Form có label và lỗi gần trường. Thao tác quan trọng dùng modal xác nhận hoặc yêu cầu lý do.
- Heading theo thứ tự, keyboard focus rõ, trạng thái không chỉ truyền đạt bằng màu.

## Tiêu chí hoàn thành

- Route và hành động đúng role/approval.
- DTO, payload và response envelope khớp Backend.
- Không có dữ liệu giả hoặc fallback giả danh người dùng.
- Đủ loading, error, empty và success state.
- Dùng được trên mobile và desktop theo nhóm người dùng.
- `npm run lint`, `npx tsc --noEmit`, `npm test` và `npm run build` thành công.
