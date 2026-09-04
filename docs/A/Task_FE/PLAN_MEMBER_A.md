# Kế hoạch Triển khai Frontend - Thành viên A

Dựa trên tài liệu `PLANFE.md` và `frontend-design-spec(1).md`, dưới đây là phân tích chi tiết và kế hoạch hành động cụ thể cho bạn (Thành viên A).

## 1. Vai trò và Trách nhiệm

Bạn chịu trách nhiệm chính về **Identity & Experience** và **Design System** cho toàn bộ dự án.

**Các feature sở hữu:**
- `auth` (Đăng nhập, đăng ký, quên mật khẩu, đặt lại mật khẩu, xác thực email, chọn role OAuth...)
- `teacher-profile` (Hồ sơ giáo viên, trạng thái phê duyệt, chứng chỉ/tài liệu, quản lý môn dạy, đề xuất môn mới...)
- `marketplace` (Tìm kiếm, danh sách giáo viên, chi tiết giáo viên, landing page...)
- `catalog` (Danh mục môn học)
- `learning` (Giao/nhận/chấm bài tập, nộp bài, file đính kèm...)
- `chat` (Nhắn tin realtime STOMP.js, quản lý hội thoại...)
- `notifications` (Thông báo, notification center, unread badge...)
- `ranking` (Bảng xếp hạng toàn hệ thống, thống kê giáo viên)
- Toàn bộ **Shared design system** (`shared/components` và `shared/lib`).

**Các route phụ trách (bạn sẽ export lazy pages để Thành viên B gắn vào Router):**

| Nhóm | Route |
|---|---|
| Auth | `/auth/login`, `/auth/register`, `/auth/verify-email`, `/auth/forgot-password`, `/auth/reset-password`, `/auth/oauth/role` |
| Public / Marketplace | `/` (Landing), `/subjects`, `/teachers`, `/teachers/:id`, `/ranking` |
| Teacher – Hồ sơ | `/teacher/profile`, `/teacher/documents`, `/teacher/subjects`, `/teacher/subject-proposals` |
| Teacher – Kinh doanh | `/teacher/packages`, `/teacher/packages/new`, `/teacher/packages/:id/edit`, `/teacher/availability` |
| Teacher – Giảng dạy (Learning) | `/teacher/assignments`, `/teacher/assignments/new`, `/teacher/assignments/:id`, `/teacher/submissions/:id` |
| Teacher – Hiệu suất | `/teacher/stats` |
| Teacher – Trao đổi | `/teacher/messages(/:conversationId)`, `/teacher/notifications` |
| Student – Learning | `/student/assignments`, `/student/assignments/:id` |
| Student – Trao đổi | `/student/messages(/:conversationId)`, `/student/notifications` |
| Student – Hồ sơ | `/student/profile` |
| System (phối hợp B) | `/403`, `/404` |

---

## 2. Kế hoạch chi tiết theo từng tuần (8 Tuần)

### Tuần 1: Xây dựng Nền tảng UI (Frontend Foundation)

Đây là tuần cực kỳ quan trọng để bạn thiết lập chuẩn mực UI cho toàn bộ dự án, bám sát `frontend-design-spec(1).md`.

- **Cấu trúc dự án:**
  - Thiết lập thư mục cho các feature sở hữu: `auth`, `marketplace`, `teacher-profile`, `catalog`, `learning`, `chat`, `notifications`, `ranking`.
  - Khởi tạo thư mục `shared/components` và `shared/lib`.

- **Design Tokens & Theme (Spec mục 10):**
  - Thiết lập Ant Design theme map với các Design Tokens (màu sắc, typography, spacing, radius, shadow, z-index, motion...).
  - Định nghĩa toàn bộ CSS variables (tokens) theo spec vào file global CSS.
  - Tích hợp 3 font chuẩn: `Be Vietnam Pro` (Heading/UI), `Inter` (Body/Data) và `JetBrains Mono` (Code/Mã giao dịch).
  - Cấu hình ánh xạ sang Ant Design theme token và component token (Spec mục 10.3).

- **Layout & Structure (Spec mục 9):**
  - Xây dựng responsive application layout:
    - **Navbar** ngang cho public/auth zone (sticky, h=64, logo, menu, auth buttons / avatar dropdown).
    - **Sidebar** cố định 248px, thu gọn 72px cho app zone (Student/Teacher). Đặc tả item theo Spec 4.6.
    - **TopHeader** 64px (breadcrumb desktop / title + back mobile, user menu, NotificationBell).
    - **BottomNavigation** 56px + safe-area cho Mobile Student/Teacher (Spec 4.8).
    - **Footer** cho public zone (nền `primary-900`, padding dọc 64px).

- **Shared Primitive Components (Spec mục 11, tuân thủ 6 states):**
  - `Button` (primary/secondary/ghost/text/danger/icon-only, 3 sizes sm/md/lg).
  - Form controls: `Input` (text/password/prefix/suffix), `Textarea` (autosize/fixed), `Checkbox` (checked/indeterminate), `Radio` (default/card), `Select` (single/multi/searchable), `Switch` (default/loading).
  - `DatePicker` / `TimeRangePicker` (date/range/time-range).
  - `Search` (inline/global/filter).
  - `Dropdown` (menu/action).
  - `Tabs` wrap (line/segmented).
  - `Modal` (480/640/800, destructive variant).
  - `Drawer` (left/right/full-screen mobile).
  - `Toast` system (success/info/warning/error, max 3 chồng, nhóm lỗi lặp).
  - `Alert` wrap (semantic variants: info/success/warning/error).
  - `Tooltip` wrap (placement variants).
  - `Breadcrumb` (collapsible cho desktop).
  - `Table` (default/compact/sticky, `tabular-nums` cho số, header bg `surface-sunken`).
  - `Pagination` (full/simple).
  - `FileUpload` (single/multiple/dropzone, kéo thả + chọn file).
  - `ConfirmDialog` (normal/danger/stale — xử lý 409 optimistic locking).

- **Shared Data Display Components:**
  - `StatusTag` — map domain status → semantic token, thống nhất toàn hệ thống (Spec 6.5).
  - `MoneyText` — format VND, `tabular-nums`, `nowrap`, variants normal/+/-/compact (Spec 1.4).
  - `DateTimeText` — format dd/MM/yyyy, HH:mm, relative time, timezone Asia/Ho_Chi_Minh (Spec 1.4).
  - `SessionCounter` — hiển thị 4 chỉ số: Còn lại / Đang giữ / Đã học / Đã hoàn (horizontal/compact).
  - `Avatar` wrap — xs→xl (24→96px), verified badge.
  - `RatingStars` — readonly/interactive.
  - `Badge` — dot/count/accent.
  - `Skeleton` — text/card/table/avatar variants, shimmer animation (respect `prefers-reduced-motion`).
  - `EmptyState` — no-data/no-search/first-use, icon nét mảnh 1.5px đơn sắc.
  - `ErrorState` — inline/page/auth variants.

- **Trang hệ thống (phối hợp B):**
  - Template trang `403 Forbidden` (ErrorState + Button quay dashboard/trang trước).
  - Template trang `404 Not Found` (ErrorState + Search optional + Button về trang chủ).

---

### Tuần 2: Auth và Onboarding

- **Auth Flow:**
  - Trang Login (`/auth/login`) — email/password, Google OAuth, link forgot password, link register. Auth shell 2 cột desktop / card mobile.
  - Trang Register (`/auth/register`) — email, password, họ tên, chọn role. Validate client + server.
  - Trang chọn role sau Google OAuth (`/auth/oauth/role`) — 2 card STUDENT/TEACHER (ADMIN không hiện).
  - Trang xác thực Email (`/auth/verify-email`) — Alert sticky "Vui lòng xác minh email" + nút gửi lại.
  - Trang quên mật khẩu (`/auth/forgot-password`) — input email, success message không tiết lộ email tồn tại.
  - Trang đặt lại mật khẩu (`/auth/reset-password`) — password mới + xác nhận, xử lý token invalid/expired.
  - Xử lý UI cho trạng thái `ACCOUNT_LOCKED` và `DISABLED` (ErrorState + lý do + nút liên hệ hỗ trợ).

- **Teacher Onboarding (Hồ sơ Giáo viên):**
  - Xây dựng wizard/form thiết lập hồ sơ giáo viên (`/teacher/profile`).
  - Trang quản lý Chứng chỉ / Tài liệu (`/teacher/documents`) — FileUpload, Table/Card, StatusTag, ConfirmDialog.
  - Trang quản lý Môn dạy (`/teacher/subjects`) — Select/Search, Card/Table, Button, ConfirmDialog.
  - Trang Đề xuất môn mới (`/teacher/subject-proposals`) — Table/Card, StatusTag, Input, Textarea, Button.
  - Xây dựng component `TeacherApprovalBanner` — hiển thị ở đầu **mọi trang** `/teacher`:
    - `DRAFT` → Info banner + nút "Gửi duyệt"
    - `PENDING_APPROVAL` → Warning banner + timestamp gửi, khóa trường nhạy cảm
    - `REJECTED` → Error banner + `rejection_reason` + nút "Chỉnh sửa và gửi lại"
    - `APPROVED` → Ẩn banner, chỉ badge ✓ ở header
  - Menu bị khóa: disabled + Tooltip "Cần được duyệt hồ sơ trước".

- **Student Profile:**
  - Trang hồ sơ cá nhân (`/student/profile`) — Avatar, thông tin cơ bản, form nhập thông tin liên hệ phụ huynh.

---

### Tuần 3: Marketplace & Ranking

- **Composite Components (dùng xuyên suốt Marketplace):**
  - `TeacherCard` (compact/full) — Avatar, tên, verified, môn, rating, số review, giá thấp nhất.
  - `SubjectCard` (default/compact) — tên, mô tả, số giáo viên.
  - `PackageCard` (public/purchased) — tên gói, giá, số buổi, mô tả.
  - `WeeklyScheduleGrid` (editable/readonly/booking-overlay) — lưới 7 ngày, TimeRangePicker.

- **Public UI:**
  - Landing page (`/`) — Hero section typography lớn, search hero → `/teachers?q=`, môn nổi bật (SubjectCard grid), giáo viên nổi bật (TeacherCard grid), quy trình 3 bước, trust section, CTA, Footer.
  - Danh mục môn học (`/subjects`) — Search, grid SubjectCard (4/3/2 cột responsive), Pagination.

- **Tìm kiếm Giáo viên (`/teachers`):**
  - Sidebar filter 280px (Desktop) / Drawer filter (Mobile/Tablet).
  - Debounced search, phân trang, sort bar.
  - Đồng bộ trạng thái filter/search với URL query params.
  - Grid TeacherCard (3/2/1 cột responsive).
  - EmptyState "Không tìm thấy giáo viên phù hợp" + nút "Xóa bộ lọc".

- **Chi tiết Giáo viên (`/teachers/:id`):**
  - Profile header (Avatar xl, Bio, verified badge, rating, subjects).
  - Tabs: Giới thiệu | Gói học (PackageCard grid) | Lịch rảnh (WeeklyScheduleGrid readonly) | Đánh giá (RatingStars, reviews list).
  - CTA "Mua gói" → Guest: modal yêu cầu đăng nhập; Student: chuyển checkout (qua public interface của B); Teacher/Admin: ẩn nút.
  - CTA "Yêu cầu học thử" → `/student/trials/new`.
  - Nút "Nhắn tin" disabled + tooltip khi chưa đủ điều kiện mở chat (Spec 3.4 mục 11).

- **Dashboard Quản lý của Giáo viên:**
  - Trang quản lý Hồ sơ giảng dạy (`/teacher/profile`) — form cập nhật bio, thông tin.
  - Trang cấu hình Lịch rảnh (`/teacher/availability`) — WeeklyScheduleGrid editable, TimeRangePicker, Switch, Modal/Drawer, Alert.
  - Trang danh sách Gói học (`/teacher/packages`) — Table, StatusTag, MoneyText, SessionCounter, Button.
  - Trang tạo/sửa Gói học (`/teacher/packages/new`, `/teacher/packages/:id/edit`) — form Input, Select, MoneyText preview, Radio/Switch, Textarea.

- **Ranking:**
  - Bảng xếp hạng cơ bản (`/ranking`) — Podium/top cards, ranking table, Avatar, RatingStars.
  - Nhấn mạnh Top 1-3 với màu Accent `#B45309`.
  - Desktop: table; Mobile: card list.

---

### Tuần 4 & 5: Giao diện Hỗ trợ Checkout & Booking

*(Logic lõi Checkout/Booking do Thành viên B làm, bạn phụ trách tích hợp trải nghiệm người dùng)*

- **Notification UX cho Thanh toán (Checkout):**
  - Toast thông báo thành công/thất bại cho quá trình thanh toán.
  - Cập nhật UI CTA ở Marketplace (teacher detail) để điều hướng sang luồng thanh toán qua public feature interface.

- **Booking UX:**
  - Form Đánh giá giáo viên (Review) — RatingStars interactive, Textarea, chỉ hiển thị khi Booking đủ điều kiện từ backend.
  - Notification UX nhắc nhở lịch học sắp tới.
  - UI thông báo khi Booking quá hạn xác nhận (countdown "Còn X giờ") hoặc bị trùng/hủy.
  - Toast/Alert cho các state transition: SCHEDULED → COMPLETED / CANCELLED / EXPIRED.

---

### Tuần 6: Learning (Bài tập) & STOMP Chat

- **Bài tập — Teacher side:**
  - Danh sách bài tập (`/teacher/assignments`) — Table, StatusTag, Search, DateTimeText, Button. Badge "bài chờ chấm".
  - Trình tạo bài tập (`/teacher/assignments/new`) — Assignment Builder: Input title, Textarea instructions, Select student/package, DatePicker due, FileUpload attachments. Save draft/publish theo API.
  - Chi tiết bài tập & danh sách nộp (`/teacher/assignments/:id`) — Assignment detail + submissions table, StatusTag.
  - Chấm bài (`/teacher/submissions/:id`) — Submission content view, grading form (Input score, Textarea feedback), Button.

- **Bài tập — Student side:**
  - Danh sách bài tập (`/student/assignments`) — Tabs To-do/Submitted/Graded, Search, Card/Table, StatusTag, DateTimeText. Badge "bài chưa nộp".
  - Chi tiết bài tập & nộp bài (`/student/assignments/:id`) — Assignment content, attachments viewer, submission editor (Textarea + FileUpload), grade/feedback display. Confirm final submission.

- **Component tải lên và xem file đính kèm (Attachment)** — dùng chung cho cả Assignment và Submission.

- **Chat (STOMP.js):**
  - `NotificationBell` component — count badge, dropdown 8 thông báo gần nhất + "Xem tất cả".
  - Layout trang chat: 2 cột Desktop (Conversation list 320px + Thread) / 1 cột Mobile (list → thread là 2 route/state).
  - Danh sách hội thoại (Conversation list) — Search, Avatar, unread badge, last message preview.
  - Khung hiển thị chi tiết tin nhắn — `ChatBubble` (own/other/system), load history REST, infinite scroll.
  - Tích hợp gửi/nhận tin qua STOMP.js realtime:
    - Local "sending" state → ack → sent.
    - Fail → icon Failed + nút Retry, không xóa message.
  - **Reconnect UX:**
    - Badge "Đang kết nối lại…" ở header khi WebSocket mất kết nối.
    - Composer chuyển read-only khi mất kết nối.
    - Refetch recent messages/unread sau khi reconnect để chống mất event.
  - Nút "Nhắn tin" disabled + tooltip khi chưa đủ điều kiện (chỉ mở sau khi có buổi học thử hoặc gói học).
  - Chat Teacher (`/teacher/messages`) và Chat Student (`/student/messages`) — cùng layout, khác route.

- **Notification Center:**
  - Trang thông báo đầy đủ (`/teacher/notifications`, `/student/notifications`) — Tabs, `NotificationItem` (read/unread/action), EmptyState, Pagination/Load more.
  - Tính năng hiển thị unread badge (chấm đỏ), đánh dấu đã đọc / đọc tất cả.
  - Deep-link: click notification → navigate đến target page tương ứng.

---

### Tuần 7: Thống kê & Hoàn thiện Ranking

- **Thống kê cho Giáo viên (`/teacher/stats`):**
  - Xây dựng các thẻ StatsCard tóm tắt (Revenue, Completed sessions, Rating, Rank).
  - Period filter (Select/DatePicker) cho thống kê.
  - Căn chỉnh hiển thị tiền tệ/số liệu đúng chuẩn (`tabular-nums`, căn phải, `nowrap`).
  - Chart placeholder cho dữ liệu revenue/sessions (chỉ thêm khi metric API xác nhận).
  - Ranking card hiển thị vị trí cá nhân.

- **Ranking Toàn hệ thống (`/ranking`):**
  - Hoàn thiện giao diện Bảng xếp hạng chi tiết.
  - Filter subject/kỳ nếu endpoint hỗ trợ.
  - Đánh dấu nhấn mạnh Top 1-3 với màu `Accent` (`#B45309`).
  - Responsive: Table desktop → card list mobile.

---

### Tuần 8: Hardening & Testing

- **Testing:**
  - Viết/hoàn thiện Component Tests cho `Auth`, `Marketplace`, `Ranking`.
  - Viết/hoàn thiện Component Tests cho `Learning`, `Chat`, `Notifications`.
  - Viết/hoàn thiện Component Tests cho `teacher-profile` (documents, subjects, proposals).

- **Quality Assurance:**
  - Rà soát Accessibility (A11y) và responsive QA cho toàn bộ shared components.
  - Kiểm tra keyboard navigation, focus management, ARIA attributes.
  - Cùng B chạy kiểm thử E2E trên các luồng người dùng chính.
  - Xử lý mượt mà mọi Loading state, Empty state, Error & Retry state.
  - Kiểm tra `prefers-reduced-motion` — tắt shimmer, giảm animation.
  - Kiểm tra đảm bảo Production build không có lỗi TypeScript hay ESLint.

---

## 3. Các yêu cầu UI/UX quan trọng từ Design Spec (Quy định Màu sắc & Layout)

- **Kiến trúc phân quyền và UI:** Ẩn hoặc disable (chặn) các nút/menu không thuộc quyền của người dùng. Luôn cung cấp tooltip hoặc thông báo tại sao chức năng bị khóa. Trang `403 Forbidden` cho truy cập URL trực tiếp.
- **Màu sắc & Typography (Thiết kế Trung tính & Tinh tế):** 
  - Nền trang (body): `--color-background` (`#FBFAF8`). Card/Surface: `--color-surface` (`#FFFFFF`).
  - Màu nhấn chính (Primary): `--color-primary-600` (`#0F766E`). Tuyệt đối chỉ dùng 1 màu Primary này cho các hành động chính, active state, và giá tiền nổi bật.
  - Màu tôn vinh (Accent): `--color-accent-500` (`#B45309`). Cực kỳ hạn chế, tối đa một vùng/màn hình (như huy hiệu Top giáo viên, Top 3 Ranking).
  - Phân cấp thông tin bằng Typography và Spacing (base 4px), **không** dùng màu sắc để phân cấp. Text chính dùng `--color-text-primary` (`#1C1917`), text phụ `--color-text-secondary` (`#57534E`).
- **Component States:** Đảm bảo mọi nút nhấn/input phải có trạng thái: Default, Hover (120ms), Active/Pressed (translateY 1px), Focus-visible (ring 3px `--shadow-focus`), Disabled (bg `--color-disabled-bg`, text `--color-text-disabled`), Loading.
- **Border mảnh thay shadow:** Dùng `1px solid var(--color-border)` (`#E7E3DC`); shadow chỉ dùng cho phần tử nổi như dropdown, modal, popover, sticky bar (`--shadow-sm` hoặc `--shadow-md`).
- **Layout & Spacing (Căn chỉnh quang học chính xác):**
  - **Grid & Container:** Container public max-width 1200px (1320px cho trang tìm kiếm).
  - **Padding ngang trang:** 32px (desktop) / 24px (tablet) / 16px (mobile).
  - **Khoảng cách section:** 96px (desktop public), 40px (dashboard desktop).
  - **Grid Gap:** 24px (desktop) / 20px (tablet) / 16px (mobile).
  - **Bo góc (Radius):** 4px (`--radius-xs`) cho tag/badge nhỏ; 8px (`--radius-md`) cho control (input/button); 12px (`--radius-lg`) cho card/alert; 16px (`--radius-xl`) cho modal. Không bo tròn kiểu "bubbly".
- **Chuyển động:** Nhanh và ngắn 120–200ms, easing `cubic-bezier(0.2, 0, 0, 1)`. Không parallax, không animation trang trí.
- **Số liệu tài chính:** Dùng font `tabular-nums`, căn phải, `white-space: nowrap`. Số tiền VND: phân cách nghìn bằng dấu chấm, hậu tố ` ₫`. Số dương `--color-success-600`, âm `--color-error-600`.
- **Empty state:** Dùng icon nét mảnh 1.5px, một màu (`--color-text-placeholder`). Text chính (`--text-h4`), phụ (`--text-secondary`). Không dùng illustration màu mè.

### Các trạng thái đặc biệt frontend phải xử lý (Spec 3.4):

| # | Trạng thái | Feature A liên quan |
|---|---|---|
| 1 | Teacher chưa duyệt → khóa menu + banner | `teacher-profile` |
| 2 | Optimistic locking 409 → ConfirmDialog "Dữ liệu đã thay đổi" | Teacher profile, packages, learning |
| 3 | Token hết hạn → toast "Phiên đăng nhập đã hết hạn" | `auth` (UI toast; interceptor do B) |
| 4 | WebSocket mất kết nối → badge "Đang kết nối lại…" | `chat` |
| 5 | Chat chưa đủ điều kiện → disable + tooltip | `chat`, `marketplace` |

---

## 4. Chống xung đột mã nguồn (Với Thành viên B)

- **Tự quản lý folder feature của mình:** Mã nguồn của feature bạn sở hữu nằm trọn vẹn trong folder của feature đó (bao gồm `api`, `components`, `hooks`, `pages`, `schemas`, `types`).
- **Không Deep Import:** Các tính năng giao tiếp thông qua file `index.ts` public của từng thư mục feature. Không lấy trực tiếp từ các file bên trong feature của B.
- **Phối hợp thay đổi API/Router:** Nếu cần sửa đổi `app/router` hoặc cấu hình Axios client `shared/api`, không tự làm gộp mà phải tạo một Pull Request riêng để thống nhất với B.
- **A không sửa** `app/router` hoặc `shared/api`. B không sửa internals của feature do A sở hữu.

Chúc bạn triển khai thành công!
