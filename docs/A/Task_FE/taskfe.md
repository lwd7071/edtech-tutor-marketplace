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
- [ ] `StatusTag` — map domain status → semantic token thống nhất (radius 4px `--radius-xs`):
  - [ ] TeacherProfile: DRAFT (neutral) / PENDING_APPROVAL (warning `--color-warning-600`) / APPROVED (success `--color-success-600`) / REJECTED (error `--color-error-600`)
  - [ ] Invoice: PENDING (warning) / PAID (success) / CANCELLED (neutral) / EXPIRED (error)
  - [ ] StudentPackage: PENDING_PAYMENT (warning) / ACTIVE (success) / COMPLETED (info `--color-info-600`) / LOCKED_EXPIRED (error) / REFUND_PENDING (warning) / REFUNDED (neutral)
  - [ ] Booking: SCHEDULED (info) / COMPLETED (success) / CANCELLED (neutral) / EXPIRED (error)
  - [ ] Payout: PENDING (warning) / PROCESSING (info) / SUCCEEDED (success) / REJECTED (neutral) / FAILED (error)
  - [ ] Refund, Extension, Assignment, Submission, Trial (theo Spec 6.5)
- [ ] `MoneyText` — format VND (phân cách nghìn bằng dấu chấm, hậu tố ` ₫`)
  - [ ] Variants: normal / +positive (success) / −negative (error) / compact (rút gọn + tooltip)
  - [ ] `tabular-nums`, `white-space: nowrap`, căn phải
  - [ ] Prefix +/− dùng Unicode (U+2212 cho minus, không dùng hyphen)
- [ ] `DateTimeText` — format theo timezone Asia/Ho_Chi_Minh, text `--color-text-secondary` hoặc `--color-text-tertiary`
  - [ ] Variants: date (`dd/MM/yyyy`), time (`HH:mm`), full (`HH:mm, dd/MM/yyyy`), range (`19:00 – 20:30 · Thứ 4, 20/08/2026`), relative (`3 phút trước`, quá 7 ngày → ngày tuyệt đối)
- [ ] `SessionCounter` — 4 chỉ số: Còn lại / Đang giữ / Đã học / Đã hoàn
  - [ ] Variants: horizontal / compact
  - [ ] Tooltip "Đang giữ": "Số buổi đã được giữ cho các booking đang lên lịch."
- [ ] `Avatar` wrap — sizes xs(24) / sm(32) / md(40) / lg(64) / xl(96), verified badge
- [ ] `RatingStars` — readonly / interactive
- [ ] `Badge` — dot (8px) / count / accent
- [ ] `Skeleton` — variants text/card/table/avatar
  - [ ] Shimmer animation 1.4s loop
  - [ ] `prefers-reduced-motion` → pulse/static
- [ ] `EmptyState` — variants no-data/no-search/first-use
  - [ ] Icon nét mảnh 1.5px đơn sắc (`--color-text-placeholder`), không illustration màu mè
  - [ ] Text chính `--text-h4`, text phụ `--text-secondary`, copy mẫu + CTA phù hợp theo Spec 20.3
- [ ] `ErrorState` — variants inline/page/auth
  - [ ] Hiển thị lỗi + nút Retry, không lộ lỗi backend nội bộ

### A1.6 Composite Components
- [ ] `TeacherCard` (compact/full) — Avatar, tên, verified, môn, RatingStars, review count, giá thấp nhất (MoneyText)
- [ ] `SubjectCard` (default/compact) — tên, mô tả ngắn, số giáo viên
- [ ] `PackageCard` (public/purchased) — tên gói, giá (MoneyText), số buổi, mô tả
- [ ] `NotificationBell` — count badge, dropdown 8 thông báo gần nhất + link "Xem tất cả"
- [ ] `NotificationItem` — read/unread/action, icon + nội dung + relative time (DateTimeText)
- [ ] `ChatBubble` — own/other/system, timestamp, sending/sent/failed state
- [ ] `WeeklyScheduleGrid` — editable (teacher availability) / readonly (public profile) / booking-overlay (lịch dạy)
  - [ ] Lưới 7 ngày desktop, agenda view mobile
  - [ ] TimeRangePicker tích hợp cho editable mode
- [ ] `TeacherApprovalBanner` — radius 12px (`--radius-lg`), padding 16px
  - [ ] DRAFT (info `--color-info-bg`, text `--color-info-600`)
  - [ ] PENDING_APPROVAL (warning `--color-warning-bg`, text `--color-warning-600`)
  - [ ] REJECTED (error `--color-error-bg`, text `--color-error-600`)
  - [ ] APPROVED (ẩn)

### A1.7 System Pages (phối hợp B)
- [ ] Template trang `403 Forbidden` (`/403`) — ErrorState centered + Button quay dashboard/trang trước
- [ ] Template trang `404 Not Found` (`/404`, `*`) — ErrorState centered + Search optional + Button về trang chủ

---

## A2: Auth và Onboarding

### A2.1 Luồng Đăng nhập / Đăng ký
- [ ] Trang Login (`/auth/login`) *(API: POST /api/auth/login)*
  - [ ] Auth shell 2 cột desktop / card 440-480px mobile
  - [ ] Input email + password (autocomplete username/current-password)
  - [ ] Checkbox "Ghi nhớ đăng nhập"
  - [ ] Nút "Đăng nhập" (primary) + "Đăng nhập bằng Google" (secondary)
  - [ ] Link "Quên mật khẩu?" + Link "Đăng ký"
  - [ ] Xử lý lỗi: `ACCOUNT_LOCKED` → ErrorState + lý do + nút "Liên hệ hỗ trợ"
  - [ ] Giữ redirect param (`?redirect=<path>`)
- [ ] Trang Register (`/auth/register`) *(API: POST /api/auth/register)*
  - [ ] Input email, password (+ rules text), họ tên, chọn role (Select/Radio)
  - [ ] Checkbox đồng ý điều khoản
  - [ ] Validate client + hiển thị validation error từ Backend
  - [ ] Google OAuth + link Login
- [ ] Trang chọn role sau Google OAuth (`/auth/oauth/role`) *(API: POST /api/auth/oauth2/complete-registration)*
  - [ ] 2 card STUDENT / TEACHER (ADMIN không hiện)
  - [ ] Giải thích hậu quả lựa chọn (không đổi được sau)
  - [ ] Radio group semantics
- [ ] Trang quên mật khẩu (`/auth/forgot-password`) *(API: POST /api/auth/forgot-password)*
  - [ ] Input email, success message không tiết lộ email tồn tại hay không
  - [ ] `aria-live` cho status message
- [ ] Trang đặt lại mật khẩu (`/auth/reset-password`) *(API: POST /api/auth/reset-password)*
  - [ ] Input password mới + xác nhận
  - [ ] Xử lý token invalid/expired → ErrorState + link gửi lại
  - [ ] Show/hide password accessible
- [ ] Trang xác thực Email (`/auth/verify-email`) *(API: POST /api/auth/verify-email, POST /api/auth/resend-verification)*
  - [ ] Alert sticky "Vui lòng xác minh email" + nút gửi lại
  - [ ] Chặn mua gói & gửi hồ sơ duyệt khi chưa verify
- [ ] Full-screen splash khi kiểm tra phiên (logo + spinner, không nháy layout)

### A2.2 Teacher Onboarding (Hồ sơ Giáo viên)
- [ ] Xây dựng wizard/form thiết lập hồ sơ giáo viên (`/teacher/profile`) *(API: GET/PUT /api/teacher/profile, POST /submit)*
  - [ ] Input: bio, thông tin cá nhân, kinh nghiệm
  - [ ] Avatar upload
  - [ ] Max-width 880px (Spec 9.3)
- [ ] Trang quản lý Chứng chỉ / Tài liệu (`/teacher/documents`) *(API: POST /api/teacher/documents, DELETE /{id})*
  - [ ] FileUpload (multiple/dropzone) cho chứng chỉ, bằng cấp
  - [ ] Table/Card danh sách tài liệu đã upload + StatusTag
  - [ ] ConfirmDialog xác nhận xóa
- [ ] Trang quản lý Môn dạy (`/teacher/subjects`) *(API: GET/POST/DELETE /api/teacher/subjects)*
  - [ ] Select/Search chọn môn từ danh mục
  - [ ] Card/Table danh sách môn đang dạy
  - [ ] Button thêm/xóa (ConfirmDialog khi xóa)
- [ ] Trang Đề xuất môn mới (`/teacher/subject-proposals`) *(API: GET/POST /api/teacher/subject-proposals)*
  - [ ] Input tên môn + Textarea mô tả
  - [ ] Table/Card lịch sử đề xuất + StatusTag (PENDING/APPROVED/REJECTED)
- [ ] Xây dựng component `TeacherApprovalBanner`
  - [ ] Hiển thị ở đầu **mọi trang** `/teacher`
  - [ ] `DRAFT`: Info banner + nút "Gửi hồ sơ duyệt"
  - [ ] `PENDING_APPROVAL`: Warning banner + timestamp gửi lúc HH:mm dd/MM, khóa sửa trường nhạy cảm
  - [ ] `REJECTED`: Error banner + `rejection_reason` + nút "Chỉnh sửa và gửi lại"
  - [ ] `APPROVED`: Ẩn banner, badge ✓ Đã duyệt ở header
  - [ ] Menu bị khóa: disabled + Tooltip "Cần được duyệt hồ sơ trước"

### A2.3 Hồ sơ Học sinh
- [ ] Trang hồ sơ cá nhân (`/student/profile`)
  - [ ] Avatar, thông tin cơ bản
  - [ ] Form nhập thông tin liên hệ phụ huynh (tên, SĐT, email)

---

## A3: Marketplace, Teacher Profile & Ranking

### A3.1 Landing Page (`/`)
- [ ] Hero section: typography lớn (--text-display 48px / mobile 32px), search bar → `/teachers?q=`
- [ ] Section môn nổi bật: SubjectCard grid (4/3/2 cột)
- [ ] Section giáo viên nổi bật: TeacherCard grid (3/2/1 cột)
- [ ] Quy trình 3 bước (Trust section)
- [ ] CTA đăng ký theo trạng thái auth (Guest → Register, Logged in → Dashboard)
- [ ] Footer
- [ ] Container max-width 1200px, padding ngang 32px (desktop) / 24px (tablet) / 16px (mobile)
- [ ] Section spacing: 96px (desktop) / 64px (tablet) / 48px (mobile)
- [ ] Nền trang `--color-background` (`#FBFAF8`), Card nền `--color-surface` (`#FFFFFF`), shadow `--shadow-sm` hoặc chỉ dùng border 1px `--color-border`

### A3.2 Danh mục môn học (`/subjects`) *(API: GET /api/public/subjects)*
- [ ] Page header + Search
- [ ] Grid SubjectCard (4/3/2 cột responsive)
- [ ] Pagination
- [ ] Click môn → `/teachers?subjectId=`
- [ ] EmptyState, Loading skeleton, Error + retry

### A3.3 Tìm kiếm Giáo viên (`/teachers`) *(API: GET /api/public/teachers)*
- [ ] Sidebar filter 280px cố định (Desktop) / Drawer filter (Mobile/Tablet)
  - [ ] Filter: môn học, rating, giá, availability, verified
  - [ ] Nút "Xóa bộ lọc"
- [ ] Sort bar (relevance, rating, price, reviews)
- [ ] Debounced search input
- [ ] Đồng bộ trạng thái filter/search/sort/page với URL query params
- [ ] Grid TeacherCard (3/2/1 cột responsive)
- [ ] Pagination
- [ ] EmptyState "Không tìm thấy giáo viên phù hợp. Thử bỏ bớt bộ lọc." + nút "Xóa bộ lọc"
- [ ] Result count live region sau lọc (accessibility)
- [ ] Container max-width 1320px

### A3.4 Chi tiết Giáo viên (`/teachers/:id`) *(API: GET /api/public/teachers/{id}, /packages, /availability, /reviews)*
- [ ] Profile header: Avatar xl, Bio, verified badge, rating (RatingStars), subjects
- [ ] Tabs:
  - [ ] **Giới thiệu** — Bio chi tiết, credentials public
  - [ ] **Gói học** — PackageCard grid (3/2/1 cột), MoneyText giá
  - [ ] **Lịch rảnh** — WeeklyScheduleGrid readonly
  - [ ] **Đánh giá** — Reviews list, RatingStars, Pagination
- [ ] CTA sticky desktop:
  - [ ] "Mua gói" → Guest: modal yêu cầu đăng nhập; Student + email verified: chuyển checkout; Teacher/Admin: ẩn
  - [ ] "Yêu cầu học thử" → `/student/trials/new`
  - [ ] "Nhắn tin" → disabled + tooltip nếu chưa đủ điều kiện (chưa có trial/package với GV này)
- [ ] Responsive: 2 cột desktop, 1 cột mobile; tabs scroll ngang

### A3.5 Dashboard Quản lý của Giáo viên
- [ ] Trang quản lý Hồ sơ giảng dạy (`/teacher/profile`) *(API: GET/PUT /api/teacher/profile)*
  - [ ] Form cập nhật bio, thông tin, kinh nghiệm
  - [ ] Max-width 880px
- [ ] Trang cấu hình Lịch rảnh (`/teacher/availability`) *(API: GET/PUT /api/teacher/availability)*
  - [ ] WeeklyScheduleGrid editable
  - [ ] TimeRangePicker thêm/sửa slot
  - [ ] Switch bật/tắt ngày
  - [ ] Modal/Drawer xác nhận thay đổi
  - [ ] Alert cảnh báo thay đổi ảnh hưởng booking
- [ ] Trang danh sách Gói học (`/teacher/packages`) *(API: GET /api/teacher/packages)*
  - [ ] Table: tên, giá (MoneyText), số buổi, trạng thái (StatusTag)
  - [ ] Button "Tạo gói học" (chỉ khi APPROVED)
  - [ ] EmptyState "Bạn chưa tạo gói học." + CTA
- [ ] Trang tạo Gói học (`/teacher/packages/new`) *(API: POST /api/teacher/packages)*
  - [ ] Form: Input tên, Select môn, Input giá (MoneyText preview), Input số buổi, Input thời hạn, Textarea mô tả
  - [ ] Radio/Switch trial enabled
  - [ ] Max-width 880px
- [ ] Trang sửa Gói học (`/teacher/packages/:id/edit`) *(API: PUT /api/teacher/packages/{id}, PATCH /status)*
  - [ ] Như tạo, load dữ liệu hiện tại
  - [ ] Xử lý trường immutable (nếu đã có người mua)

### A3.6 Bảng xếp hạng (`/ranking`) *(API: GET /api/public/teachers/ranking)*
- [ ] Header + filter subject/kỳ (nếu endpoint hỗ trợ)
- [ ] Podium / Top 3 cards với màu Accent `#B45309`
- [ ] Ranking table: rank, Avatar, tên teacher, subjects, rating (RatingStars), score
  - [ ] Highlight Top 1-3 với màu `--color-accent-500` (`#B45309`) cho huy hiệu / text nhấn, nền huy hiệu `--color-accent-50` (`#FFFBEB`)
- [ ] Pagination
- [ ] Responsive: Table desktop → card list mobile
- [ ] Thứ hạng đọc được bằng text, không chỉ huy chương (accessibility)

---

## A4 & A5: Hỗ trợ Checkout & Booking UX

*(Logic lõi Checkout/Booking do B làm, A phụ trách tích hợp trải nghiệm người dùng)*

### A4.1 Notification UX cho Thanh toán
- [ ] Toast / Alert thông báo thành công khi thanh toán hoàn tất
- [ ] Toast / Alert thông báo thất bại / hết hạn khi thanh toán lỗi
- [ ] Cập nhật CTA ở Teacher detail (`/teachers/:id`) điều hướng sang luồng checkout qua public interface

### A4.2 Booking UX
- [ ] Form Đánh giá giáo viên (Review): *(API: POST /api/student/bookings/{id}/review)*
  - [ ] RatingStars interactive + Textarea comment
  - [ ] Chỉ hiển thị khi Booking đủ điều kiện từ backend
  - [ ] Confirm trước khi submit (nếu không sửa được)
- [ ] Notification UX nhắc nhở lịch học sắp tới
- [ ] UI thông báo khi Booking quá hạn xác nhận:
  - [ ] Countdown "Còn X giờ để xác nhận" (12h kể từ end_time) cho Teacher
  - [ ] Sau đó chuyển EXPIRED
- [ ] Toast/Alert cho booking bị trùng (409 `BOOKING_TIME_CONFLICT`) hoặc bị hủy

---

## A6: Learning (Bài tập) & STOMP Chat

### A6.1 Bài tập — Teacher side
- [ ] Danh sách bài tập (`/teacher/assignments`) *(API: GET /api/teacher/assignments)*
  - [ ] Table: title, assignee/package, due (DateTimeText), submission counts, status (StatusTag)
  - [ ] Search + filter
  - [ ] Button "Tạo bài tập"
  - [ ] Badge "bài chờ chấm" ở sidebar
  - [ ] EmptyState + Loading skeleton
- [ ] Tạo bài tập (`/teacher/assignments/new`) *(API: POST /api/teacher/assignments)*
  - [ ] Assignment Builder: Input title, Textarea instructions, Select student/package/booking, DatePicker due
  - [ ] FileUpload attachments
  - [ ] Save draft / Publish theo API
  - [ ] Max-width 880px
- [ ] Chi tiết bài tập & danh sách nộp (`/teacher/assignments/:id`)
  - [ ] Assignment detail card
  - [ ] Submissions table: student, submitted time, status (StatusTag), grade
  - [ ] Button mở chấm bài
  - [ ] Edit/close assignment nếu API cho phép
- [ ] Chấm bài (`/teacher/submissions/:id`) *(API: POST /api/teacher/submissions/{id}/grade)*
  - [ ] Submission content view (text + file attachments)
  - [ ] Grading form: Input score, Textarea feedback
  - [ ] Button "Lưu điểm"
  - [ ] Max-width 880px

### A6.2 Bài tập — Student side
- [ ] Danh sách bài tập (`/student/assignments`) *(API: GET /api/student/assignments)*
  - [ ] Tabs: To-do / Submitted / Graded
  - [ ] Search + filter
  - [ ] Card/Table: title, teacher, due (DateTimeText), status (StatusTag), grade
  - [ ] Badge "bài chưa nộp" ở sidebar
  - [ ] Overdue state: text rõ ràng, không chỉ dựa màu
- [ ] Chi tiết bài tập & nộp bài (`/student/assignments/:id`) *(API: POST /api/student/assignments/{id}/submissions)*
  - [ ] Assignment content: instructions, due date, attachments viewer (download)
  - [ ] Submission editor: Textarea + FileUpload
  - [ ] Current submission / grade / feedback display
  - [ ] ConfirmDialog trước khi submit cuối cùng (irreversible)
  - [ ] Save draft nếu API hỗ trợ

### A6.3 Attachment Component (dùng chung)
- [ ] Component tải lên file đính kèm (FileUpload integration) *(API: POST /api/attachments)*
- [ ] Component xem/tải file đính kèm (preview + download link)
- [ ] Accessible names cho attachments

### A6.4 Chat (STOMP.js)
- [ ] Layout trang chat:
  - [ ] Desktop: 2 cột (Conversation list 320px + Thread)
  - [ ] Tablet: 2 cột (list 280px + Thread nếu landscape)
  - [ ] Mobile: 1 cột, list → thread là 2 route/state riêng, nút back rõ
  - [ ] Full height: `height: calc(100vh - 64px)`, scroll nội bộ
- [ ] Danh sách hội thoại (Conversation list): *(API: GET /api/conversations)*
  - [ ] Search conversations
  - [ ] Avatar + tên + last message preview + relative time (DateTimeText)
  - [ ] Unread badge (chấm/count)
- [ ] Khung hiển thị chi tiết tin nhắn (Thread): *(API: GET /api/conversations/{id}/messages)*
  - [ ] `ChatBubble` (own/other/system) với timestamp
  - [ ] Load message history qua REST API
  - [ ] Infinite scroll "Tải thêm tin cũ" (keyboard-friendly)
- [ ] Gửi tin nhắn (Composer):
  - [ ] Input + nút Send
  - [ ] Local "sending" state → ack → "sent"
  - [ ] Fail → icon Failed + nút "Thử lại", không xóa message
- [ ] Tích hợp STOMP.js realtime:
  - [ ] Subscribe conversation khi mở
  - [ ] Nhận tin nhắn mới realtime
  - [ ] `aria-live polite` cho thông báo "Có tin nhắn mới" (không cưỡng bức focus)
- [ ] **Reconnect UX:**
  - [ ] Badge "Đang kết nối lại…" ở header khi WebSocket mất kết nối (text, không chỉ spinner)
  - [ ] Composer chuyển read-only khi mất kết nối
  - [ ] Refetch recent messages/unread sau khi reconnect thành công để chống mất event
- [ ] Nút "Nhắn tin" disabled + tooltip khi chưa đủ điều kiện:
  - [ ] "Chỉ nhắn tin được sau khi có buổi học thử hoặc gói học với giáo viên này"
- [ ] Chat Teacher (`/teacher/messages(/:conversationId)`) — export lazy page
- [ ] Chat Student (`/student/messages(/:conversationId)`) — export lazy page

### A6.5 Notification Center
- [ ] Trang thông báo Teacher (`/teacher/notifications`): *(API: GET /api/notifications)*
  - [ ] Tabs filter (Tất cả, Duyệt hồ sơ, Booking, Bài tập, Tài chính, Tin nhắn...)
  - [ ] `NotificationItem` list (read/unread/action)
  - [ ] Pagination / Load more
  - [ ] EmptyState "Bạn đã xem hết thông báo."
- [ ] Trang thông báo Student (`/student/notifications`): *(API: GET /api/student/notifications)*
  - [ ] Tương tự Teacher, tabs phù hợp Student
- [ ] Tính năng unread badge:
  - [ ] Chấm đỏ trên icon Bell ở header + sidebar
  - [ ] Count badge với số lượng chưa đọc
- [ ] Đánh dấu đã đọc / đọc tất cả *(API: PATCH /api/notifications/{id}/read, POST /read-all)*
- [ ] Deep-link: click notification → navigate đến target page tương ứng
- [ ] `NotificationBell` dropdown ở header:
  - [ ] 8 thông báo gần nhất
  - [ ] Link "Xem tất cả" → `/teacher/notifications` hoặc `/student/notifications`

---

## A7: Dashboard Thống kê & Ranking Hoàn chỉnh

### A7.1 Thống kê cho Giáo viên (`/teacher/stats`) *(API: GET /api/teacher/stats)*
- [ ] StatsCard tóm tắt: Revenue, Completed sessions, Average rating, Current rank
  - [ ] Số tiền lớn: 28px / weight 700, đơn vị ₫ 16px / weight 500
  - [ ] Tooltip giá trị đầy đủ khi hiển thị rút gọn
- [ ] Period filter: Select/DatePicker chọn khoảng thời gian
- [ ] Căn chỉnh hiển thị tiền tệ/số liệu đúng chuẩn (`tabular-nums`, căn phải, `nowrap`)
- [ ] Chart placeholder cho dữ liệu revenue/sessions (thêm khi metric API xác nhận)
- [ ] Ranking card hiển thị vị trí cá nhân + so sánh
- [ ] Charts cần có text/table equivalent (accessibility)
- [ ] Responsive: Charts/cards stack mobile

### A7.2 Ranking Toàn hệ thống (`/ranking`) — Hoàn thiện
- [ ] Hoàn thiện giao diện Bảng xếp hạng chi tiết
- [ ] Filter theo subject/kỳ nếu endpoint hỗ trợ
- [ ] Đánh dấu nhấn mạnh Top 1-3 với màu `Accent` (`#B45309`)
- [ ] Responsive: Table desktop → card list mobile
- [ ] Accessibility: Thứ hạng đọc được bằng text

---

## A8: Quality Assurance & Testing

### A8.1 Component / Hook Tests
- [ ] Tests cho `Auth` (login, register, OAuth role, password flows)
- [ ] Tests cho `Marketplace` (search, filter, teacher detail, landing)
- [ ] Tests cho `Ranking` (ranking table, filter, top highlights)
- [ ] Tests cho `Learning` (assignment builder, submission, grading)
- [ ] Tests cho `Chat` (conversation list, message send/receive, reconnect)
- [ ] Tests cho `Notifications` (notification list, unread badge, mark read)
- [ ] Tests cho `teacher-profile` (documents, subjects, proposals, approval banner)

### A8.2 Shared Components QA
- [ ] Kiểm tra Accessibility (A11y) cho toàn bộ shared components:
  - [ ] Keyboard navigation (Tab/Shift+Tab, Enter/Space, Arrow keys, Escape)
  - [ ] Focus management (focus ring, focus trap modal/drawer, focus return)
  - [ ] ARIA attributes (labels, describedby, required, sort, live regions)
  - [ ] Contrast ratios (body ≥4.5:1, large text ≥3:1, non-text UI ≥3:1)
  - [ ] Color không là tín hiệu duy nhất
- [ ] Kiểm tra Responsive trên toàn bộ shared components:
  - [ ] Mobile (<640px), Tablet (640-1023px), Desktop (≥1024px), Wide (≥1440px)
  - [ ] Touch target ≥44×44px mobile
  - [ ] `env(safe-area-inset-bottom)` cho sticky bars
- [ ] Kiểm tra `prefers-reduced-motion`:
  - [ ] Tắt shimmer mạnh → pulse/static
  - [ ] Giảm translate/slide animations

### A8.3 Integration & E2E
- [ ] Chạy kiểm thử E2E các luồng liên quan tới A cùng thành viên B:
  - [ ] Luồng Auth: Register → Verify email → Login → Dashboard
  - [ ] Luồng Teacher onboarding: Register → Profile → Documents → Subjects → Submit approval
  - [ ] Luồng Marketplace: Search → Filter → Teacher detail → CTA mua gói
  - [ ] Luồng Learning: Teacher tạo bài → Student xem → Nộp bài → Teacher chấm
  - [ ] Luồng Chat: Mở conversation → Gửi tin → Nhận tin realtime
- [ ] Rà soát toàn bộ Loading state, Empty state, Error state và Retry logic
- [ ] Kiểm tra tất cả Empty state copy theo Spec 20.3
- [ ] Kiểm tra tất cả Confirmation copy theo Spec 20.4
- [ ] Clean code: Đảm bảo không còn lỗi ESLint và TypeScript trong các folder phụ trách
- [ ] Kiểm tra đảm bảo Production build (`npm run build`) thành công
