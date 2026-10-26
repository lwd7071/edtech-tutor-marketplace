# Kiểm kê page thực tế

Kiểm kê từ `frontend/src/app/**/page.tsx` ngày 2026-10-15: **71 route**, gồm **70 route sản phẩm** và `/sandbox/components` nội bộ. Route group `(public)`, `(auth)`, `(workspace)` không xuất hiện trong URL. `UI owner` là nơi Luna cần xem và viết lại layout; nếu ghi feature component thì `page.tsx` chỉ là wrapper. Cột data nêu adapter/domain, không cho phép đổi API contract. Chi tiết bố cục và nghiệm thu từng route ở `PAGE_DESIGN_PLAN.md`.

Mọi page dữ liệu phải phân biệt populated/loading/empty/error; page mutation còn có pending/success/conflict tương ứng logic hiện có. `—` dưới đây là trang nội dung tĩnh hoặc điều hướng, không phải dữ liệu giả. Quyền Student/Teacher/Admin nằm ở role layout và backend ownership.

## Public và trang hệ thống — 11

| Route | Người dùng, chức năng | UI owner | Data/API và trạng thái đặc thù |
|---|---|---|---|
| `/` | Guest và mọi role; vào luồng tìm gia sư, khám phá môn | `(public)/page.tsx`, `HeroSearch`, `SubjectGrid`, `TeacherGrid` | `shared/api/public.server`: subjects, teachers; lỗi từng nguồn tách riêng |
| `/subjects` | Guest/mọi role; chọn môn theo cấp học và từ khóa | `subjects/SubjectPickerClient.tsx` | Public subjects/detail; selected ID, URL filter, pagination, tải lỗi |
| `/teachers` | Guest/mọi role; lọc, so sánh và mở hồ sơ | `teachers/TeacherSearchClient.tsx`, `TeacherSearchResults` | Public teacher search, subjects, location; URL filters, preview, pagination |
| `/teachers/[id]` | Guest/mọi role; đọc hồ sơ, gói, lịch, review | `teachers/[id]/page.tsx` và client sections của route | Public teacher detail/packages/availability/reviews; 404 và lỗi từng vùng |
| `/ranking` | Guest/mọi role; xem xếp hạng thật | `ranking/RankingClient.tsx`, `features/ranking/components/RankingList.tsx` | Public ranking/subjects; filter môn, loading, empty/error; route hiện render tối đa 50 kết quả từ trang API đầu tiên và không có control phân trang |
| `/how-it-works` | Guest/mọi role; hiểu hành trình học và dạy | `how-it-works/page.tsx` | —; CTA tới route hiện có |
| `/become-a-tutor` | Guest/mọi role; hiểu bước đăng ký gia sư | `become-a-tutor/page.tsx` | —; CTA đăng ký role TEACHER |
| `/support` | Guest/mọi role; tìm đường hỗ trợ đúng ngữ cảnh | `support/page.tsx` | —; link tới requests/notifications/messages hiện có |
| `/terms` | Guest/mọi role; đọc điều khoản | `terms/page.tsx` | —; nội dung MVP hiện có |
| `/privacy` | Guest/mọi role; đọc chính sách riêng tư | `privacy/page.tsx` | —; nội dung MVP hiện có |
| `/forbidden` | Sai role; giải thích không có quyền | `forbidden/page.tsx`, `ErrorState` | Guard điều hướng; không rò dữ liệu resource |

## Auth — 7

| Route | Người dùng, chức năng | UI owner | Data/API và trạng thái đặc thù |
|---|---|---|---|
| `/auth/login` | Guest; email/password hoặc Google | `auth/login/page.tsx` | Auth API login, Google redirect; validation, pending, error, return URL |
| `/auth/register` | Guest; đăng ký Student/Teacher | `auth/register/page.tsx` | Auth register; role/query, parent optional Student, verification next |
| `/auth/verify-email` | Người vừa đăng ký; xác minh/gửi lại | `auth/verify-email/page.tsx` | Auth verify/resend; countdown, invalid/expired code, success |
| `/auth/forgot-password` | Guest; yêu cầu đặt lại mật khẩu | `auth/forgot-password/page.tsx` | Auth forgot-password; pending/result, không tiết lộ tài khoản |
| `/auth/reset-password` | Người có token; đặt mật khẩu mới | `auth/reset-password/page.tsx` | Auth reset-password; token invalid/expired, validation, success |
| `/auth/oauth/role` | OAuth mới; hoàn tất chọn Student/Teacher | `auth/oauth/role/page.tsx` | Auth complete-registration; role, pending/error, ownership of redirect |
| `/oauth2/callback` | OAuth redirect; đổi code lấy session | `oauth2/callback/page.tsx` | Auth OAuth exchange; progress/failure, redirect an toàn |

## Student — 15

| Route | Chức năng | UI owner | Data/API và trạng thái đặc thù |
|---|---|---|---|
| `/student` | Việc học tiếp theo và lối tắt | `student/page.tsx` | Student dashboard read model; lỗi không hiện 0 |
| `/student/packages` | Gói đã mua | `StudentPackagesPage` | Student packages; list/empty/error |
| `/student/packages/[id]` | Lượt, buổi và điều khoản gói | `StudentPackageDetailPage` | Student package detail; ownership/404 |
| `/student/checkout/[invoiceId]` | Thanh toán invoice | `CheckoutPage` | Invoice/PayOS; QR/link/polling, expiry/error |
| `/student/payment-result/[invoiceId]` | Kết quả invoice từ server | `PaymentResultPage` | Invoice status; pending/paid/failed |
| `/student/payments/callback` | Đối chiếu invoice sau khi quay lại từ cổng thanh toán | `student/payments/callback/page.tsx` | `invoiceId` query → invoice GET + 3s polling; hiển thị server status, không tin status query |
| `/student/bookings` | Xem và lọc lịch học | `BookingWorkspace` | Booking list/filter; timezone Việt Nam |
| `/student/bookings/[id]` | Chi tiết/xác nhận/đánh giá buổi | `BookingDetailPage` → `StudentBookingDetailView` | Booking/report/review/settlement; canConfirm/canReview; hủy chỉ là action Teacher; API owner vẫn theo role |
| `/student/assignments` | Bài được giao theo tiến độ | `StudentAssignmentList` | Learning assignments; progress TODO/SUBMITTED/GRADED, dueAt/status; DTO không có tên môn/gia sư |
| `/student/assignments/[id]` | Đọc đề, lưu nháp, nộp hoặc xem kết quả | `StudentAssignmentDetail` | Assignment/submission/files/feedback; DRAFT, SUBMITTED, GRADED riêng; overdue/closed/graded gate editor |
| `/student/messages` | Student; inbox hội thoại và trao đổi tin với gia sư | `ChatPage` (shell), `ChatLayout` (inbox), `ChatThread` (history/composer) | REST pagination, STOMP, attachment purpose MESSAGE; optimistic/echo/retry |
| `/student/notifications` | Đọc thông báo | `NotificationList` | Notification list/mark-read; chỉ đổi sau success |
| `/student/requests` | Theo dõi học thử/refund/extension | `StudentRequestsPage` | Read-only paginated requests; loading/empty/error per type; creation remains in existing flows |
| `/student/profile` | Tài khoản read-only và liên hệ phụ huynh | `student/profile/page.tsx` | GET/PUT parent contact; optional email notification; no account identity edit |
| `/student/session-reports` | Báo cáo buổi học | `student/session-reports/page.tsx` | Paginated read-only reports; safe external record link; booking detail link |

## Teacher — 25

| Route | Chức năng | UI owner | Data/API và trạng thái đặc thù |
|---|---|---|---|
| `/teacher` | Tổng quan trạng thái và việc dạy | `TeacherOverview` | `teacherApi.getProfile`, `bookingApi.getBookings`, `bookingApi.getTeacherTrialRequests`, `learningApi.getTeacherAssignments`, `financeApi.getWallet` |
| `/teacher/profile` | Chỉnh hồ sơ/nơi ở | `TeacherProfileForm` | Teacher profile/residence; approval transition |
| `/teacher/documents` | Tài liệu xác minh | `teacher/documents/page.tsx` | Teacher documents upload/delete/status |
| `/teacher/credentials` | Minh chứng năng lực | `teacher/credentials/page.tsx` | Credentials upload/edit/delete/status |
| `/teacher/subjects` | Môn đang dạy | `teacher/subjects/page.tsx` | Teacher subjects, catalog; approval constraints |
| `/teacher/subject-proposals` | Đề xuất môn | `teacher/subject-proposals/page.tsx` | Proposals; pending/approved/rejected/version |
| `/teacher/packages` | Danh sách gói | `teacher/packages/page.tsx`, `PackageList` | Pricing packages; approval gate |
| `/teacher/packages/create` | Tạo gói | `teacher/packages/create/page.tsx`, `PackageForm` | Package create; validation/status |
| `/teacher/packages/[id]` | Chỉnh gói | `teacher/packages/[id]/page.tsx`, `PackageForm` | Package detail/update; ownership/version |
| `/teacher/availability` | Lịch rảnh tuần | `AvailabilityEditor` | Availability; chặn save khi load lỗi |
| `/teacher/students` | Học viên đang học | `LearnerList` | Teacher learners/packages; empty/error |
| `/teacher/students/[id]` | Gói học của học viên, tạo buổi/giao bài | `LearnerList` theo route param | Teacher learner packages; ownership/404; CreateBookingModal |
| `/teacher/bookings` | Lịch dạy | `BookingWorkspace` | Booking list/calendar/status |
| `/teacher/bookings/[id]` | Báo cáo/xác nhận/khiếu nại buổi | `BookingDetailPage` → `TeacherBookingDetailView` | Booking/report/settlement; canDispute/version; Teacher view tách riêng khi Student S được triển khai |
| `/teacher/trial-requests` | Xử lý học thử | `teacher/trial-requests/page.tsx` | Trials accept/reject/schedule; version |
| `/teacher/assignments` | Danh sách bài đã giao | `AssignmentList` | Learning assignments/status |
| `/teacher/assignments/new` | Giao bài | `teacher/assignments/new/page.tsx` | Assignment create/attachment/deadline |
| `/teacher/assignments/[id]` | Xem/chỉnh bài và bài nộp | `teacher/assignments/[id]/page.tsx` | Assignment detail/submissions |
| `/teacher/submissions/[id]` | Chấm bài | `teacher/submissions/[id]/page.tsx` | Submission grade/feedback |
| `/teacher/messages` | Hội thoại | `ChatPage` | Chat REST/STOMP/attachment |
| `/teacher/notifications` | Đọc thông báo | `NotificationList` | Notification mark-read |
| `/teacher/stats` | Thống kê dạy thật | `TeacherStatsOverview` | Ranking/teacher stats; no synthetic counts |
| `/teacher/wallet` | Số dư, ledger | `TeacherWalletPage` | Finance wallet/ledger; held riêng available |
| `/teacher/bank-accounts` | Tài khoản nhận tiền | `TeacherBankAccountsPage` → `BankAccountList`, `BankAccountModal` | Dedicated bank-account screen; masked DTO, create/update version, delete `If-Match` and POST idempotency |
| `/teacher/payouts` | Tạo và theo dõi lệnh rút tiền | `TeacherPayoutsPage` → `CreatePayoutModal`, `PayoutListTable` | Wallet available/version + masked bank accounts + paginated payout requests; POST idempotency |

## Admin — 12

| Route | Chức năng | UI owner | Data/API và trạng thái đặc thù |
|---|---|---|---|
| `/admin` | Tổng quan hàng đợi | `AdminDashboardPage` | Admin dashboard/queues; lỗi không hiện 0 |
| `/admin/dashboard` | Cùng màn tổng quan `/admin` | `AdminDashboardPage` | Cùng read model, cùng kiểm thử giao diện |
| `/admin/teachers` | Duyệt gia sư | `AdminTeachersPage` | Teacher approval/documents/version |
| `/admin/credentials` | Duyệt minh chứng | `admin/credentials/page.tsx` | Credential queue/filter/status/version |
| `/admin/subjects` | Duyệt đề xuất môn | `AdminSubjectsPage` | Subject proposals/catalog/version |
| `/admin/users` | Tìm và moderation người dùng | `AdminUsersPage` | User directory paged/filter/sort; audit deep-link |
| `/admin/refunds` | Quyết định hoàn tiền | `AdminRefundsPage` | Finance refund/status/idempotency |
| `/admin/extensions` | Quyết định gia hạn | `AdminExtensionsPage` | Finance extension/status/idempotency |
| `/admin/payouts` | Quyết định rút tiền | `AdminPayoutsPage` | Finance payout/status/idempotency |
| `/admin/booking-settlements` | Quyết toán/khiếu nại | `AdminBookingSettlementsPage` | Settlement list/detail/version/dispute context |
| `/admin/settings` | Cài đặt nền tảng | `AdminSettingsPage` | Settings/version/validation |
| `/admin/audit-logs` | Tra audit | `AdminAuditLogsPage` | Audit paged/filter, targetId deep-link |

## Route nội bộ

`/sandbox/components` hiển thị thử các thành phần Ant Design. Giữ ngoài 70 route sản phẩm và ngoài bảng nghiệm thu redesign; nếu sửa component dùng chung thì có thể dùng route này để smoke test, nhưng nó không thay thế kiểm tra route thật.
