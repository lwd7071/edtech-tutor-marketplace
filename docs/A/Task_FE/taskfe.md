# Danh sách Nhiệm vụ Frontend - Thành viên A

Dưới đây là danh sách các task chi tiết được chia theo từng tuần để bạn (Thành viên A) dễ dàng theo dõi và tick hoàn thành (`[x]`).
> Nền tảng kỹ thuật đã chốt: **Next.js (App Router) + TypeScript + Ant Design 5 + TanStack Query + Axios + STOMP.js + React Hook Form**.
---

## A1: Frontend Foundation (Nền tảng UI)

### A1.1 Cấu trúc dự án
- [x] Thiết lập cấu trúc thư mục cho các tính năng của A (`auth`, `marketplace`, `teacher-profile`, `catalog`, `learning`, `chat`, `notifications`, `ranking`)
- [x] Khởi tạo thư mục `shared/components` và `shared/lib`
- [x] Đảm bảo mỗi feature tự chứa `api`, `components`, `hooks`, `pages`, `schemas`, `types`

### A1.2 Design Tokens & Theme (Spec mục 10)
- [x] Định nghĩa toàn bộ CSS variables (tokens) theo spec vào file global CSS:
  - [x] Color tokens: brand (primary-50→900), accent (50, 500), surface/text, semantic (success/warning/error/info)
  - [x] Typography tokens: font families, type scale (display → overline), weights
  - [x] Spacing tokens: space-1 (4px) → space-20 (80px)
  - [x] Radius tokens: xs (4px) → full (9999px)
  - [x] Shadow tokens: xs → lg + focus ring (`--shadow-focus: 0 0 0 3px rgba(20,184,166,.28)`)
  - [x] Border tokens (`--border-default`, `--border-strong`), Breakpoints, Z-index, Component heights, Motion (`120-200ms`)
- [x] Cấu hình Ant Design theme token (ánh xạ token → `theme.token` và `theme.components` theo Spec 10.3)
- [x] Cài đặt và import các font chữ: `Be Vietnam Pro` (400/500/600/700), `Inter` (400/500/600), `JetBrains Mono` (400/500)
- [x] Cấu hình `font-display: swap`, preconnect Google Fonts, subset `latin` + `vietnamese`

### A1.3 Layout & Structure (Spec mục 9)
- [x] Component `Navbar` cho public/auth zone:
  - [x] Sticky top, h=64, nền `--color-surface` (`#FFFFFF`), border-bottom 1px `--color-border` (`#E7E3DC`)
  - [x] Logo (→ `/`), menu giữa (`Tìm gia sư`, `Môn học`, `Bảng xếp hạng`) với text `--color-text-primary` (`#1C1917`)
  - [x] Guest: nút `Đăng nhập` (ghost) + `Đăng ký` (primary `--color-primary-600`)
  - [x] Logged in: `NotificationBell` + `Avatar` dropdown
  - [x] Shadow xuất hiện khi scroll > 8px (`--shadow-sm`)
  - [x] Mobile: hamburger drawer
- [x] Component `Sidebar` cho Dashboard (Student/Teacher):
  - [x] Cố định 248px desktop, thu gọn 72px (icon only + tooltip), nền `--color-surface`
  - [x] Đặc tả item: h=40px, gap 12px, radius 8px (`--radius-md`), 6 states (default/hover/active/disabled/collapsed)
  - [x] Hover item: bg `--color-primary-50`, text `--color-text-primary`
  - [x] Active indicator: thanh 3px bo tròn bên trái màu primary (`--color-primary-600`), bg `--color-primary-50`, text/icon `--color-primary-600`
  - [x] Group label: 11px, uppercase, letter-spacing 0.06em, text `--color-text-tertiary`
  - [x] Disabled item (chưa duyệt): opacity 0.45, cursor not-allowed, icon khóa + tooltip
  - [x] Mobile/Tablet: overlay drawer
- [x] Component `TopHeader` cho app zone:
  - [x] h=64px, sticky, z-index `--z-header` (200), nền `--color-surface`, border-bottom 1px `--color-border`
  - [x] Trái: toggle sidebar (mobile) + `Breadcrumb` (desktop) / title + back (mobile)
  - [x] Phải: `NotificationBell` + `Avatar` dropdown (tên, email, Hồ sơ, Cài đặt, Đăng xuất color `--color-error-600`)
- [x] Component `BottomNavigation` cho mobile Student/Teacher:
  - [x] h=56px + safe-area (`env(safe-area-inset-bottom)`)
  - [x] Student: Tổng quan, Gói học, Lịch học, Bài tập, Tin nhắn
  - [x] Teacher: Tổng quan, Lịch dạy, Học sinh, Ví, Thêm (drawer)
  - [x] Active: màu primary + icon filled; badge chấm 8px
- [x] Component `Footer` cho public zone:
  - [x] Nền `--color-primary-900` (`#042F2E`), padding dọc 64px (`--space-16`), text `--color-text-inverse`
  - [x] Logo, links, copyright

### A1.4 Shared Primitive Components (Spec mục 11, 6 states)

**Form Controls:**
- [x] `Button` (primary/secondary/ghost/text/danger/icon-only) × 3 sizes (sm 32px / md 40px / lg 48px)
  - [x] 6 states: default, hover (120ms, bg `--color-primary-700`), active (translateY 1px), focus-visible (ring 3px `--shadow-focus`), disabled (bg `--color-disabled-bg`, text `--color-text-disabled`), loading
  - [x] Font weight 600, radius 8px (`--radius-md`), no primaryShadow
- [x] `Input` (text/password/prefix/suffix)
  - [x] Height 40px (mobile ≥44px hit-area), border `--color-border`, radius 8px
  - [x] 6 states + error state (border `--color-error-600` + helper text `--color-error-600`)
- [x] `Textarea` (autosize/fixed, max-width 68ch cho đoạn dài)
- [x] `Checkbox` (checked/indeterminate)
- [x] `Radio` (default/card variant)
- [x] `Select` (single/multi/searchable)
- [x] `Switch` (default/loading)
- [x] `DatePicker` / `TimeRangePicker` (date/range/time-range)
- [x] `Search` (inline/global/filter, debounced)

**Overlay & Navigation:**
- [x] `Dropdown` (menu/action)
- [x] `Tabs` wrap (line/segmented)
- [x] `Modal` (width 480/640/800, destructive variant)
  - [x] Trap focus, Escape đóng, trả focus về trigger
  - [x] Mobile: full-screen sheet từ dưới
- [x] `Drawer` (left/right/full-screen mobile)
  - [x] Slide animation 200-240ms, reduced-motion → fade
- [x] `Toast` system (success/info/warning/error)
  - [x] Không chồng quá 3, nhóm lỗi lặp
  - [x] Fade/slide 8px, 180ms
  - [x] Không auto-focus, dùng `aria-live`
- [x] `Alert` wrap (info/success/warning/error semantic variants)
- [x] `Tooltip` wrap (placement variants, giải thích disabled reason)
- [x] `Breadcrumb` (collapsible cho desktop, ẩn mobile)
- [x] `ConfirmDialog` (normal/danger/stale)
  - [x] Variant stale: "Dữ liệu đã thay đổi ở nơi khác" + nút "Tải lại" (409 handling)

**Data Display:**
- [x] `Table` (default/compact/sticky header)
  - [x] Header bg `--color-surface-sunken` (`#F5F3EF`), text `--color-text-secondary`, hover row `--color-surface-hover` (`#F7F6F3`)
  - [x] Cell padding 12px 16px, `tabular-nums` cho số, border `--color-border`
  - [x] Mobile: chuyển thành card list
- [x] `Pagination` (full/simple)
- [x] `FileUpload` (single/multiple/dropzone)
  - [x] Kéo thả + chọn file, progress indicator

### A1.5 Shared Business Components (Spec mục 11.3)
- [x] `StatusTag` — map domain status → semantic token thống nhất (radius 4px `--radius-xs`):
  - [x] TeacherProfile: DRAFT (neutral) / PENDING_APPROVAL (warning `--color-warning-600`) / APPROVED (success `--color-success-600`) / REJECTED (error `--color-error-600`)
  - [x] Invoice: PENDING (warning) / PAID (success) / CANCELLED (neutral) / EXPIRED (error)
  - [x] StudentPackage: PENDING_PAYMENT (warning) / ACTIVE (success) / COMPLETED (info `--color-info-600`) / LOCKED_EXPIRED (error) / REFUND_PENDING (warning) / REFUNDED (neutral)
  - [x] Booking: SCHEDULED (info) / COMPLETED (success) / CANCELLED (neutral) / EXPIRED (error)
  - [x] Payout: PENDING (warning) / PROCESSING (info) / SUCCEEDED (success) / REJECTED (neutral) / FAILED (error)
  - [x] Refund, Extension, Assignment, Submission, Trial (theo Spec 6.5)
- [x] `MoneyText` — format VND (phân cách nghìn bằng dấu chấm, hậu tố ` ₫`)
  - [x] Variants: normal / +positive (success) / −negative (error) / compact (rút gọn + tooltip)
  - [x] `tabular-nums`, `white-space: nowrap`, căn phải
  - [x] Prefix +/− dùng Unicode (U+2212 cho minus, không dùng hyphen)
- [x] `DateTimeText` — format theo timezone Asia/Ho_Chi_Minh, text `--color-text-secondary` hoặc `--color-text-tertiary`.
  - [x] Variants: date (`dd/MM/yyyy`), time (`HH:mm`), full (`HH:mm, dd/MM/yyyy`), range (`19:00 – 20:30 · Thứ 4, 20/08/2026`), relative (`3 phút trước`, quá 7 ngày → ngày tuyệt đối)
- [x] `SessionCounter` — 4 chỉ số: Còn lại / Đang giữ / Đã học / Đã hoàn
  - [x] Variants: horizontal / compact
  - [x] Tooltip "Đang giữ": "Số buổi đã được giữ cho các booking đang lên lịch."
- [x] `Avatar` wrap — sizes xs(24) / sm(32) / md(40) / lg(64) / xl(96), verified badge
- [x] `RatingStars` — readonly / interactive
- [x] `Badge` — dot (8px) / count / accent
- [x] `Skeleton` — variants text/card/table/avatar
  - [x] Shimmer animation 1.4s loop
  - [x] `prefers-reduced-motion` → pulse/static
- [x] `EmptyState` — variants no-data/no-search/first-use
  - [x] Icon nét mảnh 1.5px đơn sắc (`--color-text-placeholder`), không illustration màu mè
  - [x] Text chính `--text-h4`, text phụ `--text-secondary`, copy mẫu + CTA phù hợp theo Spec 20.3
- [x] `ErrorState` — variants inline/page/auth
  - [x] Hiển thị lỗi + nút Retry, không lộ lỗi backend nội bộ

### A1.6 Composite Components
- [x] `TeacherCard` (compact/full) — Avatar, tên, verified, môn, RatingStars, review count, giá thấp nhất (MoneyText)
- [x] `SubjectCard` (default/compact) — tên, mô tả ngắn, số giáo viên
- [x] `PackageCard` (public/purchased) — tên gói, giá (MoneyText), số buổi, mô tả
- [x] `NotificationBell` — count badge, dropdown 8 thông báo gần nhất + link "Xem tất cả"
- [x] `NotificationItem` — read/unread/action, icon + nội dung + relative time (DateTimeText)
- [x] `ChatBubble` — own/other/system, timestamp, sending/sent/failed state
- [x] `WeeklyScheduleGrid` — editable (teacher availability) / readonly (public profile) / booking-overlay (lịch dạy)
  - [x] Lưới 7 ngày desktop, agenda view mobile
  - [x] TimeRangePicker tích hợp cho editable mode
- [x] `TeacherApprovalBanner` — radius 12px (`--radius-lg`), padding 16px
  - [x] DRAFT (info `--color-info-bg`, text `--color-info-600`)
  - [x] PENDING_APPROVAL (warning `--color-warning-bg`, text `--color-warning-600`)
  - [x] REJECTED (error `--color-error-bg`, text `--color-error-600`)
  - [x] APPROVED (ẩn)

### A1.7 System Pages (phối hợp B)
- [x] Template trang `403 Forbidden` (`/403`) — ErrorState centered + Button quay dashboard/trang trước
- [x] Template trang `404 Not Found` (`/404`, `*`) — ErrorState centered + Search optional + Button về trang chủ

---

## A2: Auth và Onboarding

### A2.1 Luồng Đăng nhập / Đăng ký
- [x] Trang Login (`/auth/login`) *(API: POST /api/auth/login)*
  - [x] Auth shell 2 cột desktop / card 440-480px mobile
  - [x] Input email + password (autocomplete username/current-password)
  - [x] Checkbox "Ghi nhớ đăng nhập"
  - [x] Nút "Đăng nhập" (primary) + "Đăng nhập bằng Google" (secondary)
  - [x] Link "Quên mật khẩu?" + Link "Đăng ký"
  - [x] Xử lý lỗi: `ACCOUNT_LOCKED` → ErrorState + lý do + nút "Liên hệ hỗ trợ"
  - [x] Giữ redirect param (`?redirect=<path>`)
- [x] Trang Register (`/auth/register`) *(API: POST /api/auth/register)*
  - [x] Input email, password (+ rules text), họ tên, chọn role (Select/Radio)
  - [x] Checkbox đồng ý điều khoản
  - [x] Validate client + hiển thị validation error từ Backend
  - [x] Google OAuth + link Login
- [x] Trang chọn role sau Google OAuth (`/auth/oauth/role`) *(API: POST /api/auth/oauth2/complete-registration)*
  - [x] 2 card STUDENT / TEACHER (ADMIN không hiện)
  - [x] Giải thích hậu quả lựa chọn (không đổi được sau)
  - [x] Radio group semantics
- [x] Trang quên mật khẩu (`/auth/forgot-password`) *(API: POST /api/auth/forgot-password)*
  - [x] Input email, success message không tiết lộ email tồn tại hay không
  - [x] `aria-live` cho status message
- [x] Trang đặt lại mật khẩu (`/auth/reset-password`) *(API: POST /api/auth/reset-password)*
  - [x] Input password mới + xác nhận
  - [x] Xử lý token invalid/expired → ErrorState + link gửi lại
  - [x] Show/hide password accessible
- [x] Trang xác thực Email (`/auth/verify-email`) *(API: POST /api/auth/verify-email, POST /api/auth/resend-verification)*
  - [x] Alert sticky "Vui lòng xác minh email" + nút gửi lại
  - [x] Chặn mua gói & gửi hồ sơ duyệt khi chưa verify
- [x] Full-screen splash khi kiểm tra phiên (logo + spinner, không nháy layout)

### A2.2 Teacher Onboarding (Hồ sơ Giáo viên)
- [x] Xây dựng wizard/form thiết lập hồ sơ giáo viên (`/teacher/profile`) *(API: GET/PUT /api/teacher/profile, POST /submit)*
  - [x] Input: bio, thông tin cá nhân, kinh nghiệm
  - [x] Avatar upload
  - [x] Max-width 880px (Spec 9.3)
- [x] Trang quản lý Chứng chỉ / Tài liệu (`/teacher/documents`) *(API: POST /api/teacher/documents, DELETE /{id})*
  - [x] FileUpload (multiple/dropzone) cho chứng chỉ, bằng cấp
  - [x] Table/Card danh sách tài liệu đã upload + StatusTag
  - [x] ConfirmDialog xác nhận xóa
- [x] Trang quản lý Môn dạy (`/teacher/subjects`) *(API: GET/POST/DELETE /api/teacher/subjects)*
  - [x] Select/Search chọn môn từ danh mục
  - [x] Card/Table danh sách môn đang dạy
  - [x] Button thêm/xóa (ConfirmDialog khi xóa)
- [x] Trang Đề xuất môn mới (`/teacher/subject-proposals`) *(API: GET/POST /api/teacher/subject-proposals)*
  - [x] Input tên môn + Textarea mô tả
  - [x] Table/Card lịch sử đề xuất + StatusTag (PENDING/APPROVED/REJECTED)
- [x] Xây dựng component `TeacherApprovalBanner`
  - [x] Hiển thị ở đầu **mọi trang** `/teacher`
  - [x] `DRAFT`: Info banner + nút "Gửi hồ sơ duyệt"
  - [x] `PENDING_APPROVAL`: Warning banner + timestamp gửi lúc HH:mm dd/MM, khóa sửa trường nhạy cảm
  - [x] `REJECTED`: Error banner + `rejection_reason` + nút "Chỉnh sửa và gửi lại"
  - [x] `APPROVED`: Ẩn banner, badge ✓ Đã duyệt ở header
  - [x] Menu bị khóa: disabled + Tooltip "Cần được duyệt hồ sơ trước"

### A2.3 Hồ sơ Học sinh
- [x] Trang hồ sơ cá nhân (`/student/profile`)
  - [x] Avatar, thông tin cơ bản
  - [x] Form nhập thông tin liên hệ phụ huynh (tên, SĐT, email)

---

## A3: Marketplace, Teacher Profile & Ranking

### A3.1 Landing Page (`/`)
- [x] Hero section: typography lớn (--text-display 48px / mobile 32px), search bar → `/teachers?q=`
- [x] Section môn nổi bật: SubjectCard grid (4/3/2 cột)
- [x] Section giáo viên nổi bật: TeacherCard grid (3/2/1 cột)
- [x] Quy trình 3 bước (Trust section)
- [x] CTA đăng ký theo trạng thái auth (Guest → Register, Logged in → Dashboard)
- [x] Footer
- [x] Container max-width 1200px, padding ngang 32px (desktop) / 24px (tablet) / 16px (mobile)
- [x] Section spacing: 96px (desktop) / 64px (tablet) / 48px (mobile)
- [x] Nền trang `--color-background` (`#FBFAF8`), Card nền `--color-surface` (`#FFFFFF`), shadow `--shadow-sm` hoặc chỉ dùng border 1px `--color-border`

### A3.2 Danh mục môn học (`/subjects`) *(API: GET /api/public/subjects)*
- [x] Page header + Search
- [x] Grid SubjectCard (4/3/2 cột responsive)
- [x] Pagination
- [x] Click môn → `/teachers?subjectId=`
- [x] EmptyState, Loading skeleton, Error + retry

### A3.3 Tìm kiếm Giáo viên (`/teachers`) *(API: GET /api/public/teachers)*
- [x] Pagination
- [x] EmptyState "Không tìm thấy giáo viên phù hợp. Thử bỏ bớt bộ lọc." + nút "Xóa bộ lọc"
- [x] Result count live region sau lọc (accessibility)
- [x] Container max-width 1320px

### A3.4 Chi tiết Giáo viên (`/teachers/:id`) *(API: GET /api/public/teachers/{id}, /packages, /availability, /reviews)*
- [x] Profile header: Avatar xl, Bio, verified badge, rating (RatingStars), subjects
- [x] Tabs:
  - [x] **Giới thiệu** — Bio chi tiết, credentials public
  - [x] **Gói học** — PackageCard grid (3/2/1 cột), MoneyText giá
  - [x] **Lịch rảnh** — WeeklyScheduleGrid readonly
  - [x] **Đánh giá** — Reviews list, RatingStars, Pagination
- [x] CTA sticky desktop:
  - [x] "Mua gói" → Guest: modal yêu cầu đăng nhập; Student + email verified: chuyển checkout; Teacher/Admin: ẩn
  - [x] "Yêu cầu học thử" → `/student/trials/new`
  - [x] "Nhắn tin" → disabled + tooltip nếu chưa đủ điều kiện (chưa có trial/package với GV này)
- [x] Responsive: 2 cột desktop, 1 cột mobile; tabs scroll ngang

### A3.5 Dashboard Quản lý của Giáo viên
- [x] Trang quản lý Hồ sơ giảng dạy (`/teacher/profile`) *(API: GET/PUT /api/teacher/profile)*
  - [x] Form cập nhật bio, thông tin, kinh nghiệm
  - [x] Max-width 880px
- [x] Trang cấu hình Lịch rảnh (`/teacher/availability`) *(API: GET/PUT /api/teacher/availability)*
  - [x] WeeklyScheduleGrid editable
  - [x] TimeRangePicker thêm/sửa slot
  - [x] Switch bật/tắt ngày
  - [x] Modal/Drawer xác nhận thay đổi
  - [x] Alert cảnh báo thay đổi ảnh hưởng booking
- [x] Trang danh sách Gói học (`/teacher/packages`) *(API: GET /api/teacher/packages)*
  - [x] Table: tên, giá (MoneyText), số buổi, trạng thái (StatusTag)
  - [x] Button "Tạo gói học" (chỉ khi APPROVED)
  - [x] EmptyState "Bạn chưa tạo gói học." + CTA
- [x] Trang tạo Gói học (`/teacher/packages/new`) *(API: POST /api/teacher/packages)*
  - [x] Form: Input tên, Select môn, Input giá (MoneyText preview), Input số buổi, Input thời hạn, Textarea mô tả
  - [x] Radio/Switch trial enabled
  - [x] Max-width 880px
- [x] Trang sửa Gói học (`/teacher/packages/:id/edit`) *(API: PUT /api/teacher/packages/{id}, PATCH /status)*
  - [x] Như tạo, load dữ liệu hiện tại
  - [x] Xử lý trường immutable (nếu đã có người mua)

### A3.6 Bảng xếp hạng (`/ranking`) *(API: GET /api/public/teachers/ranking)*
- [x] Header + filter subject/kỳ (nếu endpoint hỗ trợ)
- [x] Podium / Top 3 cards với màu Accent `#B45309`
- [x] Ranking table: rank, Avatar, tên teacher, subjects, rating (RatingStars), score
  - [x] Highlight Top 1-3 với màu `--color-accent-500` (`#B45309`) cho huy hiệu / text nhấn, nền huy hiệu `--color-accent-50` (`#FFFBEB`)
- [x] Pagination
- [x] Responsive: Table desktop → card list mobile
- [x] Thứ hạng đọc được bằng text, không chỉ huy chương (accessibility)

---

## A4 & A5: Hỗ trợ Checkout & Booking UX

*(Logic lõi Checkout/Booking do B làm, A phụ trách tích hợp trải nghiệm người dùng)*

### A4.1 Notification UX cho Thanh toán
- [x] Toast / Alert thông báo thành công khi thanh toán hoàn tất
- [x] Toast / Alert thông báo thất bại / hết hạn khi thanh toán lỗi
- [x] Cập nhật CTA ở Teacher detail (`/teachers/:id`) điều hướng sang luồng checkout qua public interface

### A4.2 Booking UX
- [x] Form Đánh giá giáo viên (Review): *(API: POST /api/student/bookings/{id}/review)*
  - [x] RatingStars interactive + Textarea comment
  - [x] Chỉ hiển thị khi Booking đủ điều kiện từ backend
  - [x] Confirm trước khi submit (nếu không sửa được)
- [x] Notification UX nhắc nhở lịch học sắp tới
- [x] UI thông báo khi Booking quá hạn xác nhận:
  - [x] Countdown "Còn X giờ để xác nhận" (12h kể từ end_time) cho Teacher
  - [x] Sau đó chuyển EXPIRED
- [x] Toast/Alert cho booking bị trùng (409 `BOOKING_TIME_CONFLICT`) hoặc bị hủy

---

## A6: Learning (Bài tập) & STOMP Chat

### A6.1 Bài tập — Teacher side
- [x] Danh sách bài tập (`/teacher/assignments`) *(API: GET /api/teacher/assignments)*
  - [x] Table: title, assignee/package, due (DateTimeText), submission counts, status (StatusTag)
  - [x] Search + filter
  - [x] Button "Tạo bài tập"
  - [x] Badge "bài chờ chấm" ở sidebar
  - [x] EmptyState + Loading skeleton
- [x] Tạo bài tập (`/teacher/assignments/new`) *(API: POST /api/teacher/assignments)*
  - [x] Assignment Builder: Input title, Textarea instructions, Select student/package/booking, DatePicker due
  - [x] FileUpload attachments
  - [x] Save draft / Publish theo API
  - [x] Max-width 880px
- [x] Chi tiết bài tập & danh sách nộp (`/teacher/assignments/:id`)
  - [x] Assignment detail card
  - [x] Submissions table: student, submitted time, status (StatusTag), grade
  - [x] Button mở chấm bài
  - [x] Edit/close assignment nếu API cho phép
- [x] Chấm bài (`/teacher/submissions/:id`) *(API: POST /api/teacher/submissions/{id}/grade)*
  - [x] Submission content view (text + file attachments)
  - [x] Grading form: Input score, Textarea feedback
  - [x] Button "Lưu điểm"
  - [x] Max-width 880px

### A6.2 Bài tập — Student side
- [x] Danh sách bài tập (`/student/assignments`) *(API: GET /api/student/assignments)*
  - [x] Tabs: To-do / Submitted / Graded
  - [x] Search + filter
  - [x] Card/Table: title, teacher, due (DateTimeText), status (StatusTag), grade
  - [x] Badge "bài chưa nộp" ở sidebar
  - [x] Overdue state: text rõ ràng, không chỉ dựa màu
- [x] Chi tiết bài tập & nộp bài (`/student/assignments/:id`) *(API: POST /api/student/assignments/{id}/submissions)*
  - [x] Assignment content: instructions, due date, attachments viewer (download)
  - [x] Submission editor: Textarea + FileUpload
  - [x] Current submission / grade / feedback display
  - [x] ConfirmDialog trước khi submit cuối cùng (irreversible)
  - [x] Save draft nếu API hỗ trợ

### A6.3 Attachment Component (dùng chung)
- [x] Component tải lên file đính kèm (FileUpload integration) *(API: POST /api/attachments)*
- [x] Component xem/tải file đính kèm (preview + download link)
- [x] Accessible names cho attachments

### A6.4 Chat (STOMP.js)
- [x] Layout trang chat:
  - [x] Desktop: 2 cột (Conversation list 320px + Thread)
  - [x] Tablet: 2 cột (list 280px + Thread nếu landscape)
  - [x] Mobile: 1 cột, list → thread là 2 route/state riêng, nút back rõ
  - [x] Full height: `height: calc(100vh - 64px)`, scroll nội bộ
- [x] Danh sách hội thoại (Conversation list): *(API: GET /api/conversations)*
  - [x] Search conversations
  - [x] Avatar + tên + last message preview + relative time (DateTimeText)
  - [x] Unread badge (chấm/count)
- [x] Khung hiển thị chi tiết tin nhắn (Thread): *(API: GET /api/conversations/{id}/messages)*
  - [x] `ChatBubble` (own/other/system) với timestamp
  - [x] Load message history qua REST API
  - [x] Infinite scroll "Tải thêm tin cũ" (keyboard-friendly)
- [x] Gửi tin nhắn (Composer):
  - [x] Input + nút Send
  - [x] Local "sending" state → ack → "sent"
  - [x] Fail → icon Failed + nút "Thử lại", không xóa message
- [x] Tích hợp STOMP.js realtime:
  - [x] Subscribe conversation khi mở
  - [x] Nhận tin nhắn mới realtime
  - [x] `aria-live polite` cho thông báo "Có tin nhắn mới" (không cưỡng bức focus)
- [x] **Reconnect UX:**
  - [x] Badge "Đang kết nối lại…" ở header khi WebSocket mất kết nối (text, không chỉ spinner)
  - [x] Composer chuyển read-only khi mất kết nối
  - [x] Refetch recent messages/unread sau khi reconnect thành công để chống mất event
- [x] Nút "Nhắn tin" disabled + tooltip khi chưa đủ điều kiện:
  - [x] "Chỉ nhắn tin được sau khi có buổi học thử hoặc gói học với giáo viên này"
- [x] Chat Teacher (`/teacher/messages(/:conversationId)`) — export lazy page
- [x] Chat Student (`/student/messages(/:conversationId)`) — export lazy page

### A6.5 Notification Center
- [x] Trang thông báo Teacher (`/teacher/notifications`): *(API: GET /api/notifications)*
  - [x] Tabs filter (Tất cả, Duyệt hồ sơ, Booking, Bài tập, Tài chính, Tin nhắn...)
  - [x] `NotificationItem` list (read/unread/action)
  - [x] Pagination / Load more
  - [x] EmptyState "Bạn đã xem hết thông báo."
- [x] Trang thông báo Student (`/student/notifications`): *(API: GET /api/student/notifications)*
  - [x] Tương tự Teacher, tabs phù hợp Student
- [x] Tính năng unread badge:
  - [x] Chấm đỏ trên icon Bell ở header + sidebar
  - [x] Count badge với số lượng chưa đọc
- [x] Đánh dấu đã đọc / đọc tất cả *(API: PATCH /api/notifications/{id}/read, POST /read-all)*
- [x] Deep-link: click notification → navigate đến target page tương ứng
- [x] `NotificationBell` dropdown ở header:
  - [x] 8 thông báo gần nhất
  - [x] Link "Xem tất cả" → `/teacher/notifications` hoặc `/student/notifications`

---

## A7: Dashboard Thống kê & Ranking Hoàn chỉnh

### A7.1 Thống kê cho Giáo viên (`/teacher/stats`) *(API: GET /api/teacher/stats)*
- [x] StatsCard tóm tắt: Revenue, Completed sessions, Average rating, Current rank
  - [x] Số tiền lớn: 28px / weight 700, đơn vị ₫ 16px / weight 500
  - [x] Tooltip giá trị đầy đủ khi hiển thị rút gọn
- [x] Period filter: Select/DatePicker chọn khoảng thời gian
- [x] Căn chỉnh hiển thị tiền tệ/số liệu đúng chuẩn (`tabular-nums`, căn phải, `nowrap`)
- [x] Chart placeholder cho dữ liệu revenue/sessions (thêm khi metric API xác nhận)
- [x] Ranking card hiển thị vị trí cá nhân + so sánh
- [x] Charts cần có text/table equivalent (accessibility)
- [x] Responsive: Charts/cards stack mobile

### A7.2 Ranking Toàn hệ thống (`/ranking`) — Hoàn thiện
- [x] Hoàn thiện giao diện Bảng xếp hạng chi tiết
- [x] Filter theo subject/kỳ nếu endpoint hỗ trợ
- [x] Đánh dấu nhấn mạnh Top 1-3 với màu `Accent` (`#B45309`)
- [x] Responsive: Table desktop → card list mobile
- [x] Accessibility: Thứ hạng đọc được bằng text

---

## A8: Quality Assurance & Testing

### A8.2 Shared Components QA
- [x] Kiểm tra Accessibility (A11y) cho toàn bộ shared components:
  - [x] Keyboard navigation (Tab/Shift+Tab, Enter/Space, Arrow keys, Escape)
  - [x] Focus management (focus ring, focus trap modal/drawer, focus return)
  - [x] ARIA attributes (labels, describedby, required, sort, live regions)
  - [x] Contrast ratios (body ≥4.5:1, large text ≥3:1, non-text UI ≥3:1)
  - [x] Color không là tín hiệu duy nhất
- [x] Kiểm tra Responsive trên toàn bộ shared components:
  - [x] Mobile (<640px), Tablet (640-1023px), Desktop (≥1024px), Wide (≥1440px)
  - [x] Touch target ≥44×44px mobile
  - [x] `env(safe-area-inset-bottom)` cho sticky bars
- [x] Kiểm tra `prefers-reduced-motion`:
  - [x] Tắt shimmer mạnh → pulse/static
  - [x] Giảm translate/slide animations

### A8.3 Integration & E2E
- [x] Chạy kiểm thử E2E các luồng liên quan tới A cùng thành viên B (luồng Auth, Teacher onboarding, Marketplace, Learning, Chat)
- [x] Rà soát toàn bộ Loading state, Empty state, Error state và Retry logic
- [x] Kiểm tra tất cả Empty state copy theo Spec 20.3
- [x] Kiểm tra tất cả Confirmation copy theo Spec 20.4
- [x] Clean code: Đảm bảo không còn lỗi ESLint và TypeScript trong các folder phụ trách
- [x] Kiểm tra đảm bảo Production build (`npm run build`) thành công
