# Tiến độ chuyển route

Chỉ theo dõi trạng thái route; không thay thế [docs/STATUS.md](../STATUS.md). Source inventory gốc gồm 70 route sản phẩm + 1 route sandbox. `/teacher/booking-requests` là route workflow bổ sung theo ADR 0017, không đổi mẫu số parity của 70 route legacy. Route động tính một lần.

Trạng thái hợp lệ: `Chưa bắt đầu`, `Đang làm`, `Bị chặn`, `Hoàn thành`. Landing page `/` đang được dựng lại sau đợt nghiệm thu trước; vì vậy route này cần nghiệm thu lại trước khi tiếp tục ghi nhận đủ 70/70.

Kết quả sweep trước được ghi nhận trong repo, nhưng đợt kiểm tra cuối hiện tại chưa độc lập xác minh lại toàn bộ route. Đặc biệt, không tìm thấy runner/log bền vững chứng minh JavaScript đã tắt; kiểm tra no-JS qua browser bị CDP permission gate chặn và không được thay bằng một browser automation khác. Vì vậy các nhãn `Hoàn thành` bên dưới là trạng thái từ sweep trước, không phải bằng chứng no-JS được tái xác nhận trong lượt này.

Ảnh tồn tại trong `acceptance-20261029/` là bằng chứng hình ảnh của sweep trước, không tự chứng minh cấu hình JS, role/session, CSRF hay toàn bộ 7 viewport. Có 345 ảnh cũ trong nhóm route và 2 ảnh mới cho `/`; chưa có manifest ánh xạ ảnh với từng route/trạng thái.

| Route | Thymeleaf | Ghi chú |
|---|---|---|
| `/` | Đang làm | Focused tests PASS 8/8. Hai ảnh desktop/mobile tại `acceptance-20261029/home/home-{desktop,mobile}.png` đã được mở và xem; giao diện hiển thị đủ các phần chính, avatar và footer. No-JS chưa được xác minh trực tiếp; route chưa hoàn tất theo tiêu chí nghiệm thu. |
| `/teachers` | Hoàn thành | SSR thật, GET filters giữ query, empty/error, MockMvc/browser/no-JS; link hồ sơ hoạt động tới `/teachers/{id}`; 4 ảnh tại `acceptance-20261029/public/`. |
| `/teachers/{id}` | Hoàn thành | SSR hồ sơ gia sư, lịch, gói học, review. Guest/Student access, form chọn gói POST+CSRF, form học thử POST+CSRF; 7 viewports không tràn; 4 ảnh tại `acceptance-20261029/public/`. |
| `/subjects` | Hoàn thành | SSR catalog/search/level filter/paging; MockMvc/browser/no-JS GET filter; link sang `/teachers` hoạt động; 4 ảnh tại `acceptance-20261029/public/`. |
| `/ranking` | Hoàn thành | SSR ranking/filter/page; PostgreSQL query và Redis record cache chạy được; MockMvc/browser/no-JS filter, 7 viewports 320–1440 đã xác minh; 4 ảnh tại `acceptance-20261029/public/`. |
| `/how-it-works` | Hoàn thành | SSR ba bước đúng flow; MockMvc/browser JS on/off, focus/contrast và 7 viewports đã xác minh; 4 ảnh tại `acceptance-20261029/public/`. |
| `/become-a-tutor` | Hoàn thành | SSR quy trình 3 bước, CTA đăng ký mở `/auth/register?role=TEACHER`, link tiếp tục tới `/teacher/profile`; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/public/`. |
| `/terms` | Hoàn thành | SSR nội dung điều khoản, no-JS, MockMvc/browser 7 viewports đã xác minh; 4 ảnh tại `acceptance-20261029/public/`. |
| `/privacy` | Hoàn thành | SSR chính sách bảo mật, no-JS, MockMvc/browser 7 viewports đã xác minh; 4 ảnh tại `acceptance-20261029/public/`. |
| `/support` | Hoàn thành | SSR trung tâm trợ giúp, breadcrumb/footer và link hỗ trợ; 7 viewports 320–1440 không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/public/`. |
| `/auth/login` | Hoàn thành | SSR form login POST+CSRF, Google OAuth CTA, keyboard focus, alert error, 7 viewports 320–1440 không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/auth/`. |
| `/auth/register` | Hoàn thành | SSR form đăng ký, role selector (TEACHER/STUDENT), CSRF, validation fields, 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/auth/`. |
| `/auth/verify-email` | Hoàn thành | SSR xác minh email, token handling, resend notice, generic alert focus, 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/auth/`. |
| `/auth/forgot-password` | Hoàn thành | SSR quên mật khẩu, email validation, CSRF, generic alert focus, 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/auth/`. |
| `/auth/reset-password` | Hoàn thành | SSR đặt lại mật khẩu, password input autocomplete new-password, CSRF, 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/auth/`. |
| `/auth/oauth/role` | Hoàn thành | SSR chọn vai trò OAuth khi chưa có pending session, alert hướng dẫn login, 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/auth/`. |
| `/oauth2/callback` | Hoàn thành | Compatibility GET route redirect về `/auth/login`, URL sanitization, 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/auth/`. |
| `/forbidden` | Hoàn thành | SSR 403 Forbidden page, guest/authenticated view, 7 viewports 320–1440 không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/public/`. |
| `/student` | Hoàn thành | SSR tổng quan học tập, session Student, buổi học sắp tới, gói học, Guest redirect login, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/packages` | Hoàn thành | SSR danh sách gói học của học viên, status GET filter, pagination, empty state, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/packages/{id}` | Hoàn thành | SSR chi tiết gói học, thông tin giáo viên, lượt học, form đặt lịch POST+CSRF, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/checkout/{invoiceId}` | Hoàn thành | SSR trang thanh toán hóa đơn, thông tin gói học, số tiền, provider giả, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/payment-result/{invoiceId}` | Hoàn thành | SSR kết quả thanh toán PENDING/PAID, server validation, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/payments/callback` | Hoàn thành | Callback xử lý kết quả thanh toán, redirect về payment result hợp lệ, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/bookings` | Hoàn thành | SSR danh sách buổi học, filter GET theo trạng thái, empty state, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/bookings/{id}` | Hoàn thành | SSR chi tiết buổi học, link phòng học, thời gian, hủy buổi, unowned 404, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/assignments` | Hoàn thành | SSR danh sách bài tập, progress GET filter, empty state TODO/GRADED/SUBMITTED, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/assignments/{id}` | Hoàn thành | SSR chi tiết bài tập, form nộp bài POST+CSRF, negative unowned trả HTML 404 view, responsive 320–1440 không tràn sau khi sửa CSS grid, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/session-reports` | Hoàn thành | SSR báo cáo buổi học, feedback từ giáo viên, pagination, empty state, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/messages` | Hoàn thành | SSR tin nhắn học viên, danh sách cuộc hội thoại, gửi tin POST+CSRF, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/notifications` | Hoàn thành | SSR thông báo học viên, filter UNREAD/SYSTEM, đánh dấu đã đọc POST+CSRF, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/requests` | Hoàn thành | SSR yêu cầu hoàn tiền/gia hạn/học thử, native POST+CSRF, tab switching, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/student/profile` | Hoàn thành | SSR thông tin cá nhân học viên, cập nhật thông tin phụ huynh POST+CSRF, Guest redirect, Teacher 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/student/`. |
| `/teacher` | Hoàn thành | SSR tổng quan gia sư, thống kê nhanh, yêu cầu đặt buổi PENDING, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/profile` | Hoàn thành | SSR hồ sơ gia sư, cập nhật bio/học vị/tỉnh thành POST+CSRF, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/documents` | Hoàn thành | SSR xác minh danh tính/CCCD, form upload multipart POST+CSRF, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/credentials` | Hoàn thành | SSR bằng cấp/chứng chỉ, thêm mới/sửa/xóa POST+CSRF, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/subjects` | Hoàn thành | SSR môn dạy và danh mục môn học, thêm/xóa môn dạy POST+CSRF, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/subject-proposals` | Hoàn thành | SSR đề xuất môn mới, form tạo đề xuất POST+CSRF, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/packages` | Hoàn thành | SSR danh sách gói học của gia sư, chuyển đổi trạng thái POST+CSRF, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/packages/create` | Hoàn thành | SSR tạo gói học mới, chọn môn, số buổi, giá tiền, validation, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/packages/{id}` | Hoàn thành | SSR chi tiết gói học, cập nhật nội dung POST+CSRF, khóa khi đã có học viên mua, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/availability` | Hoàn thành | SSR lịch rảnh hàng tuần, thêm/xóa khung giờ POST+CSRF, validation timezone, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/students` | Hoàn thành | SSR danh sách học viên đang theo học, gói đang active, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/students/{studentId}` | Hoàn thành | SSR chi tiết học viên, tiến độ các gói học, unowned 404 HTML view, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/bookings` | Hoàn thành | SSR lịch dạy, lọc theo ngày/trạng thái, 7 viewports 320–1440 không tràn sau khi sửa CSS flex-wrap, Guest redirect, Student 403, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/bookings/{id}` | Hoàn thành | SSR chi tiết buổi học của gia sư, gửi báo cáo, xác nhận, unowned 404, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/booking-requests` | Hoàn thành | SSR danh sách yêu cầu đặt lịch, duyệt/từ chối POST+CSRF, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/trial-requests` | Hoàn thành | SSR danh sách yêu cầu học thử, chấp nhận/từ chối POST+CSRF, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/assignments` | Hoàn thành | SSR danh sách bài tập đã giao, phân trang, thông tin nộp bài, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/assignments/new` | Hoàn thành | SSR giao bài tập mới, chọn học viên, môn học, hạn nộp, tệp đính kèm, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/assignments/{id}` | Hoàn thành | SSR chi tiết bài tập, sửa lỗi Jackson JsonNode SpEL `contentBlocks`, chỉnh sửa nháp, xuất bản, đóng bài, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/submissions/{id}` | Hoàn thành | SSR bài nộp của học viên, form chấm điểm và phản hồi POST+CSRF, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/messages` | Hoàn thành | SSR hộp thư gia sư, nhắn tin với học viên, phân trang, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/notifications` | Hoàn thành | SSR thông báo gia sư, lọc chưa đọc/hệ thống, đánh dấu đã đọc POST+CSRF, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/stats` | Hoàn thành | SSR thống kê thu nhập, số buổi dạy, đánh giá, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/wallet` | Hoàn thành | SSR ví tiền gia sư, số dư khả dụng, lịch sử biến động, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/bank-accounts` | Hoàn thành | SSR tài khoản ngân hàng nhận tiền, mask số tài khoản, thêm tài khoản POST+CSRF, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/teacher/payouts` | Hoàn thành | SSR yêu cầu rút tiền, tạo yêu cầu rút mới POST+CSRF, lịch sử rút tiền, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/teacher/`. |
| `/admin` | Hoàn thành | Alias SSR cùng `/admin/dashboard`; queue/zero/error states, no-store, no-JS, Guest redirect, Student 403; 7 viewports không tràn; 4 ảnh tại `acceptance-20261029/admin/`. |
| `/admin/dashboard` | Hoàn thành | SSR AdminDashboardService từ session Admin; metrics thật, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/admin/`. |
| `/admin/teachers` | Hoàn thành | SSR danh sách duyệt hồ sơ gia sư, approve/reject POST+CSRF+PRG, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/admin/`. |
| `/admin/credentials` | Hoàn thành | SSR duyệt minh chứng năng lực, approve/reject POST+CSRF+PRG, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/admin/`. |
| `/admin/subjects` | Hoàn thành | SSR duyệt đề xuất môn học, approve/reject POST+CSRF+PRG, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/admin/`. |
| `/admin/users` | Hoàn thành | SSR danh bạ người dùng, lọc, khóa/mở tài khoản POST+CSRF+PRG, link audit logs, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/admin/`. |
| `/admin/refunds` | Hoàn thành | SSR đối soát yêu cầu hoàn tiền, duyệt hoàn tiền POST+CSRF+PRG, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/admin/`. |
| `/admin/extensions` | Hoàn thành | SSR duyệt gia hạn khóa học, approve/reject POST+CSRF+PRG, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/admin/`. |
| `/admin/payouts` | Hoàn thành | SSR duyệt lệnh chi trả cho gia sư, xác nhận chuyển khoản POST+CSRF+PRG, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/admin/`. |
| `/admin/booking-settlements` | Hoàn thành | SSR quyết toán buổi học, xác nhận giải ngân POST+CSRF+PRG, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/admin/`. |
| `/admin/settings` | Hoàn thành | SSR cấu hình phí nền tảng, chính sách, lưu cấu hình POST+CSRF+PRG, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/admin/`. |
| `/admin/audit-logs` | Hoàn thành | SSR nhật ký kiểm toán hành vi quản trị, lọc theo actor/action, Guest redirect, Student 403; 7 viewports không tràn, JS on/off; 4 ảnh tại `acceptance-20261029/admin/`. |
| `/sandbox/components` | Chưa bắt đầu | 1 route nội bộ phục vụ sandbox linh kiện UI, không tính trong mẫu số 70 route sản phẩm |

## Trục 1 — Nghiệm thu route bằng browser (69/70 được ghi nhận từ sweep trước; `/` chưa hoàn tất)

69 route còn lại được ghi nhận hoàn thành từ sweep trước trên Spring Boot test runtime (`http://127.0.0.1:18087`); đợt cuối này chưa chạy lại toàn bộ. Trang chủ `/` đã đổi cấu trúc sau đợt sweep. No-JS cho các route chưa được tái xác minh do browser gate; không coi ảnh cũ hoặc nội dung template không có script là bằng chứng browser đã tắt JavaScript.
- **Public & Auth (17/18 được ghi nhận từ sweep trước)**: Báo cáo cũ ghi SSR/role/form/responsive/A11y; `/` đang làm lại. Các khẳng định no-JS chưa tái xác minh.
- **Student (15 route được ghi nhận từ sweep trước)**: Báo cáo cũ ghi role/ownership/SSR/responsive và ảnh; no-JS chưa tái xác minh.
- **Teacher (26 route được ghi nhận từ sweep trước, gồm 1 route bổ sung)**: Báo cáo cũ ghi SSR/role/ownership/responsive và các sửa lỗi ở `/teacher/bookings` cùng `/teacher/assignments/{id}`; no-JS chưa tái xác minh.
- **Admin (12 route được ghi nhận từ sweep trước)**: Báo cáo cũ ghi SSR/role/responsive và ảnh; no-JS chưa tái xác minh.

## Trục 2 — Sẵn sàng cutover

Cutover được đánh giá độc lập với việc nghiệm thu từng route. Nghiệm thu route 70/70 là tiền đề quan trọng, nhưng chuyển giao traffic toàn bộ sang production còn đòi hỏi:
- Cấu hình callback provider thật (VNPay/MoMo/VietQR) và Google OAuth client production.
- Topology Redis shared session store production theo ADR 0016.
- Smoke test trên staging trước khi chuyển DNS/traffic cutover.

## Số liệu hiện tại

- Public product: 9/10 route đã nghiệm thu; `/` đang làm lại.
- Auth: 7/7 route đã nghiệm thu đầy đủ.
- Student: 15/15 route đã nghiệm thu đầy đủ.
- Teacher: 26/26 route đã nghiệm thu đầy đủ.
- Admin: 12/12 route đã nghiệm thu đầy đủ.
- Internal sandbox: 1 route (không tính feature parity).

Tiến độ được ghi nhận: **69/70 route sản phẩm từ sweep trước**; `/` đang làm lại. Đây chưa phải nghiệm thu lại 70/70 theo toàn bộ tiêu chí hiện hành. Breakdown 18 + 15 + 26 + 12 = 71 route vì nhóm Teacher có thêm `/teacher/booking-requests` (route workflow bổ sung, ngoài mẫu số 70 route legacy). 345 ảnh cũ không có manifest route/trạng thái; 2 ảnh trang chủ mới được lưu riêng.
