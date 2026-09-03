# FRONTEND DESIGN SPECIFICATION
## Nền tảng kết nối gia sư 1-1 (Student ↔ Teacher Marketplace)

> Phiên bản: 1.0 · Ngày: 02/09/2026 · Ngôn ngữ: Tiếng Việt (thuật ngữ kỹ thuật giữ tiếng Anh)
> Nền tảng kỹ thuật đã chốt: **Next.js (App Router) + TypeScript + Ant Design 5 + TanStack Query + Axios + STOMP.js + React Hook Form**.
> Tài liệu này KHÔNG thay đổi business logic của Project Specification. Mọi điểm chưa rõ được đánh dấu **NEEDS CLARIFICATION**.

---

## MỤC LỤC

1. [Project Overview](#1-project-overview)
2. [User Roles](#2-user-roles)
3. [Information Architecture](#3-information-architecture)
4. [Navigation](#4-navigation)
5. [Design Direction](#5-design-direction)
6. [Color System](#6-color-system)
7. [Typography](#7-typography)
8. [Spacing System](#8-spacing-system)
9. [Layout System](#9-layout-system)
10. [Design Tokens](#10-design-tokens)
11. [Global Component System](#11-global-component-system)
12. [Page Specifications](#12-page-specifications)
13. [Forms](#13-forms)
14. [Tables](#14-tables)
15. [Modals / Drawers](#15-modals--drawers)
16. [User Flows](#16-user-flows)
17. [Responsive Design](#17-responsive-design)
18. [Accessibility](#18-accessibility)
19. [Micro Interactions](#19-micro-interactions)
20. [Content Guidelines](#20-content-guidelines)
21. [Page Inventory](#21-page-inventory)
22. [Component Inventory](#22-component-inventory)
23. [Open Questions](#23-open-questions)

---

## 1. PROJECT OVERVIEW

### 1.1. Mục tiêu hệ thống
Marketplace kết nối trực tiếp học sinh và giáo viên dạy kèm 1-1:
- Học sinh: tìm giáo viên → xem lịch rảnh → mua gói học (payOS/VietQR) → học → làm bài tập → đánh giá.
- Giáo viên: dựng hồ sơ → được Admin duyệt → chọn môn → bán gói → tạo lịch → dạy → báo cáo buổi học → rút tiền.
- Nền tảng: thu hoa hồng **5%** trên giá trị các buổi học **đã hoàn thành**.
- Admin: duyệt giáo viên, môn học, refund, gia hạn, payout; cấu hình hệ thống; xử lý vi phạm.

### 1.2. Đối tượng người dùng
| Nhóm | Đặc điểm | Hệ quả cho UI |
| :-- | :-- | :-- |
| Học sinh (12–22 tuổi) | Dùng mobile nhiều, quen app tiêu dùng | Mobile-first cho luồng tìm kiếm/thanh toán/lịch học |
| Phụ huynh | **Không đăng nhập**, chỉ nhận email | Không có màn hình nào cho phụ huynh; chỉ có form nhập thông tin liên hệ trong hồ sơ Student |
| Giáo viên | Dùng desktop nhiều (soạn bài, chấm bài, tài chính) | Desktop-first cho dashboard/lịch/ví; vẫn responsive |
| Admin | Nội bộ, desktop | Bảng dữ liệu dày, ưu tiên tốc độ thao tác, không cần mobile tối ưu |

### 1.3. Phạm vi frontend (MVP)
Có: Auth + Google OAuth2, duyệt giáo viên, danh mục môn, hồ sơ & availability, tìm kiếm/lọc/ranking, gói học, checkout payOS, booking 1-1, buổi học thử, session report, assignment/submission/attachment, chat 1-1 + notification realtime, wallet/ledger/payout/refund, dashboard quản trị.

Không có (không thiết kế màn hình): multi-tenant, subscription Free/Pro, lớp nhóm/chat nhóm, học sinh tự tạo booking, payout tự động, tự tạo phòng Zoom/Meet, video call nhúng, kế toán đầy đủ.

### 1.4. Quy ước hiển thị (áp dụng toàn hệ thống)
- **Thời gian**: API trả ISO-8601; frontend luôn hiển thị theo `Asia/Ho_Chi_Minh`.
  - Ngày: `dd/MM/yyyy` · Giờ: `HH:mm` · Đầy đủ: `HH:mm, dd/MM/yyyy`
  - Khoảng buổi học: `19:00 – 20:30 · Thứ 4, 20/08/2026`
  - Relative time chỉ dùng trong chat và notification (`3 phút trước`), quá 7 ngày quay về ngày tuyệt đối.
- **Tiền tệ**: chỉ VND, số nguyên, phân cách nghìn bằng dấu chấm, hậu tố ` ₫`. Ví dụ `1.000.000 ₫`. Không hiển thị số lẻ thập phân. Không rút gọn (`1tr`) trong bảng tài chính; chỉ được rút gọn trong StatsCard dashboard và phải có tooltip giá trị đầy đủ.
- **UUID**: không bao giờ hiển thị nguyên bản cho người dùng cuối. Chỉ hiển thị mã nghiệp vụ (`invoice_number`, `payos_order_code`) hoặc 8 ký tự đầu dạng `#a1b2c3d4` kèm nút copy trong màn hình Admin.
- **Số buổi**: luôn hiển thị dạng nhóm 4 chỉ số `Còn lại / Đang giữ / Đã học / Đã hoàn` (component `SessionCounter`).

---

## 2. USER ROLES

### 2.1. Bốn role
| Role | Đăng nhập | Ghi chú |
| :-- | :-- | :-- |
| `GUEST` | Không | Chỉ xem public pages |
| `STUDENT` | Có | Mua gói, học, nộp bài, refund/gia hạn, review |
| `TEACHER` | Có | Bị chặn nghiệp vụ bán hàng cho tới khi `TeacherProfile.status = APPROVED` |
| `ADMIN` | Có | Không đăng ký công khai; seed nội bộ |

### 2.2. Ma trận quyền → hệ quả UI
| Chức năng | Guest | Student | Teacher | Admin | Hệ quả UI khi không có quyền |
| :-- | :-: | :-: | :-: | :-: | :-- |
| Xem môn học / giáo viên / ranking | ✓ | ✓ | ✓ | ✓ | — |
| Mua gói học | ✗ | ✓ | ✗ | ✗ | Guest: nút "Mua gói" → modal yêu cầu đăng nhập. Teacher/Admin: ẩn nút |
| Tạo / hủy / hoàn thành Booking | ✗ | ✗ | ✓ | ✗ | Student chỉ xem lịch, không có nút hành động |
| Quản lý PricingPackage | ✗ | ✗ | ✓ | ✗ | Ẩn menu |
| Đề xuất môn mới | ✗ | ✗ | ✓ | ✗ | Ẩn menu |
| Làm / nộp bài tập | ✗ | ✓ | ✗ | ✗ | — |
| Giao / chấm bài | ✗ | ✗ | ✓ | ✗ | — |
| Yêu cầu refund / gia hạn | ✗ | ✓ | ✗ | ✗ | — |
| Yêu cầu payout | ✗ | ✗ | ✓ | ✗ | — |
| Duyệt giáo viên / môn / refund / payout | ✗ | ✗ | ✗ | ✓ | Ẩn toàn bộ khu `/admin` |
| Khóa tài khoản, cấu hình hệ thống | ✗ | ✗ | ✗ | ✓ | Ẩn |

**Nguyên tắc UI**: ẩn menu/nút với role không có quyền, nhưng vẫn phải có trang `403 Forbidden` cho trường hợp truy cập URL trực tiếp. Frontend **không** là lớp bảo mật — mọi ownership do backend kiểm tra; UI chỉ giảm nhiễu.

### 2.3. Trạng thái tài khoản (`User.status`) → hành vi UI
| Status | Hành vi |
| :-- | :-- |
| `PENDING_VERIFICATION` | Đăng nhập được nhưng hiển thị `Alert` sticky "Vui lòng xác minh email" + nút gửi lại. Chặn mua gói & gửi hồ sơ duyệt. |
| `ACTIVE` | Bình thường |
| `LOCKED` | Không đăng nhập được → trang Login hiện `ErrorState` với `ACCOUNT_LOCKED`, hiển thị lý do khóa, nút "Liên hệ hỗ trợ" |
| `DISABLED` | Như `LOCKED`, thông điệp "Tài khoản đã ngừng hoạt động" |

### 2.4. Trạng thái hồ sơ giáo viên (`TeacherProfile.profile_status`) → hành vi UI
```text
DRAFT → PENDING_APPROVAL → APPROVED
                         → REJECTED → PENDING_APPROVAL
APPROVED → DRAFT
```
Component `TeacherApprovalBanner` hiển thị ở đầu **mọi trang** trong `/teacher`:
| Status | Màu banner | Nội dung | Khóa chức năng |
| :-- | :-- | :-- | :-- |
| `DRAFT` | Info | "Hồ sơ đang ở bản nháp. Hoàn thiện và gửi duyệt để bắt đầu dạy." + nút "Gửi duyệt" | Môn dạy, Gói học, Booking, Ví, Payout |
| `PENDING_APPROVAL` | Warning | "Hồ sơ đang chờ Admin duyệt (gửi lúc HH:mm dd/MM)." | Như trên + khóa sửa các trường nhạy cảm |
| `REJECTED` | Error | "Hồ sơ bị từ chối: {rejection_reason}" + nút "Chỉnh sửa và gửi lại" | Như trên |
| `APPROVED` | Ẩn banner (chỉ badge ✓ Đã duyệt ở header) | — | Mở toàn bộ |

Các mục menu bị khóa hiển thị dạng disabled + `Tooltip` "Cần được duyệt hồ sơ trước".

---

## 3. INFORMATION ARCHITECTURE

### 3.1. Bản đồ khu vực
```text
/                          Public zone      (Guest + mọi role đăng nhập)
/auth/*                    Auth zone        (Guest, redirect nếu đã đăng nhập)
/student/*                 Student zone     (role STUDENT)
/teacher/*                 Teacher zone     (role TEACHER, + approval guard)
/admin/*                   Admin zone       (role ADMIN)
/403 /404                  System pages
```

### 3.2. Cây trang
```text
Public
├── /                                      Landing
├── /subjects                              Danh mục môn học
├── /teachers                              Tìm kiếm & lọc giáo viên
├── /teachers/:id                          Hồ sơ giáo viên (tab: Giới thiệu | Gói học | Lịch rảnh | Đánh giá)
├── /ranking                               Bảng xếp hạng toàn nền tảng
Auth
├── /auth/login
├── /auth/register
├── /auth/oauth/role                       Chọn vai trò sau Google OAuth
├── /auth/forgot-password
├── /auth/reset-password
Student
├── /student                               Dashboard
├── /student/packages                      Gói đã mua
├── /student/packages/:id                  Chi tiết gói
├── /student/checkout/:invoiceId           Checkout payOS (QR)
├── /student/payment-result/:invoiceId     Kết quả thanh toán
├── /student/bookings                      Lịch học
├── /student/bookings/:id                  Chi tiết buổi học + SessionReport
├── /student/trials/new                    Yêu cầu học thử
├── /student/refunds/new                   Yêu cầu refund
├── /student/extensions/new                Yêu cầu gia hạn
├── /student/requests                      Theo dõi refund/gia hạn/trial
├── /student/assignments                   Danh sách bài tập
├── /student/assignments/:id               Chi tiết & nộp bài
├── /student/messages(/:conversationId)    Chat
├── /student/notifications
└── /student/profile                       Hồ sơ + liên hệ phụ huynh
Teacher
├── /teacher                               Dashboard
├── /teacher/profile                       Hồ sơ & tiến độ duyệt
├── /teacher/documents                     Chứng chỉ / tài liệu
├── /teacher/subjects                      Môn đang dạy
├── /teacher/subject-proposals             Đề xuất môn mới
├── /teacher/packages                      Danh sách gói
├── /teacher/packages/new · /:id/edit      Tạo / sửa gói
├── /teacher/availability                  Lịch rảnh
├── /teacher/schedule                      Lịch dạy (calendar + list)
├── /teacher/bookings/new                  Tạo Booking
├── /teacher/bookings/:id                  Chi tiết Booking (hoàn thành / hủy)
├── /teacher/students                      Danh sách học sinh
├── /teacher/students/:id                  Chi tiết học sinh
├── /teacher/assignments                   Bài tập
├── /teacher/assignments/new · /:id        Tạo bài tập / chi tiết + danh sách nộp
├── /teacher/submissions/:id               Chấm bài
├── /teacher/wallet                        Ví & ledger
├── /teacher/bank-accounts                 Tài khoản ngân hàng
├── /teacher/payouts                       Yêu cầu rút tiền
├── /teacher/stats                         Thống kê & ranking
├── /teacher/messages(/:conversationId)
└── /teacher/notifications
Admin
├── /admin                                 Dashboard
├── /admin/teacher-approvals(/:id)
├── /admin/subject-proposals(/:id)
├── /admin/subjects
├── /admin/users(/:id)
├── /admin/refunds(/:id)
├── /admin/extensions(/:id)
├── /admin/payouts(/:id)
├── /admin/audit-logs
└── /admin/settings
```

### 3.3. Quan hệ giữa các trang (liên kết chéo chính)
```text
/teachers ──chọn giáo viên──► /teachers/:id ──chọn gói──► /student/checkout/:invoiceId
                                    │                              │
                                    └──"Học thử"──► /student/trials/new    ▼
                                                              /student/payment-result/:invoiceId
                                                                           │
                                                                           ▼
/student/packages ◄──── /student/packages/:id ────► /student/refunds/new | /student/extensions/new
        ▲                        │
        │                        └──► /student/bookings/:id ──► review · session report · assignment
/teacher/students/:id ──► /teacher/bookings/new (prefill studentPackageId)
/teacher/schedule ──► /teacher/bookings/:id ──► modal Hoàn thành (SessionReport) | modal Hủy
/teacher/wallet ──► /teacher/payouts ──► (Admin) /admin/payouts/:id
```

### 3.4. Trạng thái đặc biệt frontend phải xử lý
1. **Chờ webhook payOS**: sau khi quét QR, frontend **không** được tin `returnUrl`. Phải polling `GET /api/student/invoices/{id}` (mỗi 3s, tối đa 5 phút) cho đến khi `PAID` / `EXPIRED` / `CANCELLED`.
2. **Teacher chưa duyệt**: khóa menu + banner (mục 2.4).
3. **Gói `LOCKED_EXPIRED`**: vẫn hiển thị booking `SCHEDULED` đã tạo (không bị hủy), nhưng chặn tạo booking mới; hiện CTA "Gia hạn" / "Yêu cầu hoàn tiền".
4. **Gói `REFUND_PENDING`**: khóa mọi thao tác tạo booking, hiện tag cảnh báo.
5. **Booking quá `end_time` chưa xác nhận**: hiển thị countdown "Còn X giờ để xác nhận" (12h kể từ end_time) cho Teacher; sau đó chuyển `EXPIRED`.
6. **Optimistic locking 409**: hiện `ConfirmDialog` "Dữ liệu đã thay đổi ở nơi khác" + nút "Tải lại".
7. **Conflict lịch 409 `BOOKING_TIME_CONFLICT`**: highlight đỏ trường thời gian + liệt kê buổi bị trùng.
8. **Ngoài availability**: khi tạo booking, hiện `Alert warning` inline nhưng **vẫn cho submit**.
9. **Token hết hạn**: interceptor Axios tự refresh; nếu refresh fail → xóa state, redirect `/auth/login?redirect=<path>` + toast "Phiên đăng nhập đã hết hạn".
10. **WebSocket mất kết nối**: badge nhỏ "Đang kết nối lại…" ở header; chat chuyển sang chế độ chỉ đọc.
11. **Chat chưa đủ điều kiện mở**: nút "Nhắn tin" disabled + tooltip "Chỉ nhắn tin được sau khi có buổi học thử hoặc gói học với giáo viên này".

---

## 4. NAVIGATION

### 4.1. Kiến trúc điều hướng theo zone
| Zone | Desktop | Mobile |
| :-- | :-- | :-- |
| Public / Auth | Top navbar ngang, không sidebar | Top bar + hamburger drawer |
| Student / Teacher | Sidebar trái cố định 248px + top header 64px | Bottom navigation 5 mục + hamburger drawer cho mục phụ |
| Admin | Sidebar trái 248px (collapse 72px) + top header | Drawer (không bottom nav — admin không tối ưu mobile) |

### 4.2. Public navbar
- Trái: Logo (đưa về `/`).
- Giữa: `Tìm gia sư` · `Môn học` · `Bảng xếp hạng`.
- Phải (guest): `Đăng nhập` (ghost) + `Đăng ký` (primary).
- Phải (đã đăng nhập): `NotificationBell` + `Avatar` dropdown → `Bảng điều khiển`, `Hồ sơ`, `Đăng xuất`.
- Sticky top, nền `--color-surface`, border-bottom 1px, shadow xuất hiện khi scroll > 8px.

### 4.3. Sidebar — Student
```text
[Logo]
── HỌC TẬP
   ▸ Tổng quan            /student            icon: LayoutDashboard
   ▸ Gói học của tôi      /student/packages   icon: Package
   ▸ Lịch học             /student/bookings   icon: CalendarDays
   ▸ Bài tập              /student/assignments icon: NotebookPen   [badge: số bài chưa nộp]
── TRAO ĐỔI
   ▸ Tin nhắn             /student/messages   icon: MessageCircle  [badge: chưa đọc]
   ▸ Thông báo            /student/notifications icon: Bell        [badge: chưa đọc]
── YÊU CẦU
   ▸ Yêu cầu của tôi      /student/requests   icon: FileClock
── TÀI KHOẢN
   ▸ Hồ sơ                /student/profile    icon: User
```

### 4.4. Sidebar — Teacher
```text
[Logo] [Badge trạng thái duyệt]
── TỔNG QUAN
   ▸ Tổng quan            /teacher
── HỒ SƠ
   ▸ Hồ sơ giảng dạy      /teacher/profile
   ▸ Chứng chỉ            /teacher/documents
   ▸ Môn dạy              /teacher/subjects
   ▸ Đề xuất môn mới      /teacher/subject-proposals
── KINH DOANH            (khóa nếu chưa APPROVED)
   ▸ Gói học              /teacher/packages
   ▸ Lịch rảnh            /teacher/availability
── GIẢNG DẠY             (khóa nếu chưa APPROVED)
   ▸ Lịch dạy             /teacher/schedule    [badge: buổi cần xác nhận]
   ▸ Học sinh             /teacher/students
   ▸ Bài tập              /teacher/assignments [badge: bài chờ chấm]
── TÀI CHÍNH             (khóa nếu chưa APPROVED)
   ▸ Ví của tôi           /teacher/wallet
   ▸ Rút tiền             /teacher/payouts
   ▸ Tài khoản ngân hàng  /teacher/bank-accounts
── HIỆU SUẤT
   ▸ Thống kê & xếp hạng  /teacher/stats
── TRAO ĐỔI
   ▸ Tin nhắn · Thông báo
```

### 4.5. Sidebar — Admin
```text
[Logo] ADMIN
── TỔNG QUAN
   ▸ Dashboard            /admin
── DUYỆT
   ▸ Duyệt giáo viên      /admin/teacher-approvals  [badge: pending]
   ▸ Đề xuất môn học      /admin/subject-proposals  [badge]
── TÀI CHÍNH
   ▸ Hoàn tiền            /admin/refunds            [badge]
   ▸ Gia hạn gói          /admin/extensions         [badge]
   ▸ Rút tiền             /admin/payouts            [badge]
── QUẢN LÝ
   ▸ Người dùng           /admin/users
   ▸ Môn học              /admin/subjects
── HỆ THỐNG
   ▸ Nhật ký              /admin/audit-logs
   ▸ Cấu hình             /admin/settings
```

### 4.6. Đặc tả sidebar item
| Thuộc tính | Giá trị |
| :-- | :-- |
| Height | 40px · padding `0 12px` · gap icon–text 12px · radius `--radius-md` (8px) |
| Icon | 18px, `--color-text-secondary` |
| Text | `--font-size-sm` (14px), weight 500 |
| Group label | 11px, weight 600, uppercase, letter-spacing 0.06em, `--color-text-tertiary`, margin `20px 12px 6px` |
| Default | transparent |
| Hover | bg `--color-primary-50`, text `--color-text-primary` |
| Active | bg `--color-primary-50`, text `--color-primary-600`, icon `--color-primary-600`, thanh 3px bo tròn bên trái màu primary |
| Disabled (chưa duyệt) | opacity 0.45, cursor not-allowed, icon khóa 14px bên phải, tooltip |
| Collapsed (72px) | chỉ icon, căn giữa, tooltip bên phải hiện tên |

### 4.7. Top header (zone có sidebar)
Chiều cao 64px, nền surface, border-bottom 1px, sticky, z-index `--z-header`.
- Trái: nút toggle sidebar (mobile/tablet) · `Breadcrumb` (desktop, ẩn ở mobile) hoặc tiêu đề trang (mobile).
- Phải: `Search` toàn cục (chỉ Admin) · `NotificationBell` (dropdown 8 thông báo gần nhất + "Xem tất cả") · `Avatar` dropdown (tên, email rút gọn, `Hồ sơ`, `Cài đặt`, divider, `Đăng xuất` màu danger).

### 4.8. Mobile navigation
- **Student bottom nav** (5 mục, height 56px + safe-area): Tổng quan · Gói học · Lịch học · Bài tập · Tin nhắn. Mục còn lại nằm trong drawer từ avatar.
- **Teacher bottom nav**: Tổng quan · Lịch dạy · Học sinh · Ví · Thêm (mở drawer full menu).
- **Admin**: không bottom nav; hamburger → drawer trái 280px.
- Bottom nav item: icon 22px + label 11px; active = màu primary + icon filled; badge chấm 8px góc trên phải icon.

### 4.9. Route guard
| Guard | Điều kiện | Hành vi khi fail |
| :-- | :-- | :-- |
| `PublicRoute` | luôn cho qua | — |
| `GuestOnlyRoute` (`/auth/*`) | chưa đăng nhập | redirect về dashboard theo role |
| `AuthRoute` | có access token hợp lệ | redirect `/auth/login?redirect=<path>` + toast |
| `RoleRoute(role)` | `user.role === role` | redirect `/403` |
| `TeacherApprovedRoute` | `profile_status === APPROVED` | redirect `/teacher/profile` + toast "Cần được duyệt hồ sơ trước khi dùng chức năng này" |
| `EmailVerifiedRoute` (checkout, submit profile) | `email_verified === true` | Modal "Xác minh email" + nút gửi lại |

Trong lúc kiểm tra phiên khi load app: hiển thị full-screen splash với logo + spinner (không nháy layout).

---

## 5. DESIGN DIRECTION

**Từ khóa: tinh tế.** Giao diện phục vụ hai loại việc rất khác nhau — mua bán giáo dục (cần tin cậy, ấm áp) và vận hành tài chính (cần chính xác, đọc nhanh). Định hướng:

1. **Nền trung tính ấm, không trắng gắt** — `#FBFAF8` thay vì `#FFFFFF` cho page background; card mới là màu trắng thuần → tạo chiều sâu bằng chính vật liệu, không cần shadow nặng.
2. **Một màu primary duy nhất** — xanh teal trầm `#0F766E`. Đủ nghiêm túc cho tài chính, đủ ấm cho giáo dục, khác biệt với biển xanh SaaS mặc định. Accent duy nhất là hổ phách `#B45309` dùng cực kỳ tiết chế (badge nổi bật, highlight ranking).
3. **Hierarchy bằng typography và spacing, không bằng màu** — trang mặc định chỉ có 3 mức chữ và 2 mức nền. Màu chỉ xuất hiện khi mang **ý nghĩa trạng thái**.
4. **Border mảnh thay shadow** — 1px `#E7E3DC`; shadow chỉ dùng cho phần tử nổi (dropdown, modal, popover, sticky bar).
5. **Bo góc vừa** — 8px cho control, 12px cho card, 16px cho modal. Không bo tròn kiểu "bubbly".
6. **Chuyển động ngắn và ít** — 120–200ms, easing `cubic-bezier(0.2, 0, 0, 1)`. Không parallax, không animation trang trí.
7. **Số liệu tài chính là công dân hạng nhất** — dùng font tabular-nums, căn phải, không bao giờ bị cắt.
8. **Không dùng illustration màu mè**; empty state dùng icon nét mảnh 1.5px, một màu.

Những gì **cố tình không làm**: gradient nhiều màu, glassmorphism, neon, ảnh stock cười giả, icon trộn nhiều bộ, hơn 2 font.

---

## 6. COLOR SYSTEM

### 6.1. Brand & Primary
| Token | HEX | RGB | Vai trò | KHÔNG dùng khi |
| :-- | :-- | :-- | :-- | :-- |
| `--color-primary-50` | `#F0FDFA` | 240,253,250 | Nền active sidebar, nền tag nhẹ, hover row | Không dùng làm nền trang chính |
| `--color-primary-100` | `#CCFBF1` | 204,251,241 | Nền badge, progress track | Không dùng cho text |
| `--color-primary-500` | `#14B8A6` | 20,184,166 | Focus ring, icon nhấn, chart line | Không dùng cho text trên nền trắng (contrast 2.1:1 — fail) |
| `--color-primary-600` | `#0F766E` | 15,118,110 | **Primary chính**: button, link, active state, giá tiền nổi bật | Không dùng cho nền diện tích lớn > 40% viewport |
| `--color-primary-700` | `#115E59` | 17,94,89 | Hover/pressed của primary button | Không dùng làm màu mặc định |
| `--color-primary-900` | `#042F2E` | 4,47,46 | Nền footer, nền hero tối | Không dùng cho border |

### 6.2. Accent
| Token | HEX | RGB | Vai trò | KHÔNG dùng khi |
| :-- | :-- | :-- | :-- | :-- |
| `--color-accent-500` | `#B45309` | 180,83,9 | Huy hiệu "Top giáo viên", nhấn ranking #1–#3, badge "Mới" | Không dùng cho button chính, không dùng cho trạng thái (dễ nhầm warning) |
| `--color-accent-50` | `#FFFBEB` | 255,251,235 | Nền huy hiệu | Không dùng cho card thường |

> Quy tắc vàng: **một màn hình chỉ có tối đa một vùng accent**.

### 6.3. Neutral / Surface / Text
| Token | HEX | Vai trò | KHÔNG dùng khi |
| :-- | :-- | :-- | :-- |
| `--color-background` | `#FBFAF8` | Nền trang (body) | Không dùng cho card (mất tương phản lớp) |
| `--color-surface` | `#FFFFFF` | Card, modal, sidebar, header, table body | Không dùng cho nền trang |
| `--color-surface-sunken` | `#F5F3EF` | Nền section phụ, table header, code block, chat pane | Không dùng cho card nổi |
| `--color-surface-hover` | `#F7F6F3` | Hover row / hover list item | Không dùng làm nền tĩnh |
| `--color-border` | `#E7E3DC` | Border card, divider, input border | Không dùng cho text |
| `--color-border-strong` | `#D3CEC4` | Border input hover, border bảng dày | Không dùng mặc định |
| `--color-text-primary` | `#1C1917` | Heading, nội dung chính, số tiền | Không dùng cho text phụ |
| `--color-text-secondary` | `#57534E` | Mô tả, label, nội dung phụ | Không dùng cho heading |
| `--color-text-tertiary` | `#8A837B` | Caption, timestamp, group label | Không dùng cho nội dung cần đọc kỹ (contrast 4.6:1 — chỉ đạt AA với ≥14px) |
| `--color-text-placeholder` | `#A8A29E` | Placeholder input | Không dùng cho text thật (contrast 3.0:1 — fail cho body) |
| `--color-text-inverse` | `#FFFFFF` | Text trên nền primary/dark | Không dùng trên nền sáng |
| `--color-text-disabled` | `#B9B3AB` | Text trong control disabled | Không dùng để "làm nhạt" nội dung thật |

### 6.4. Semantic (trạng thái)
| Token | HEX | Nền nhạt | Vai trò | KHÔNG dùng khi |
| :-- | :-- | :-- | :-- | :-- |
| `--color-success-600` | `#15803D` | `#F0FDF4` | Thanh toán thành công, buổi học hoàn thành, payout SUCCEEDED | Không dùng cho nút "Lưu" thông thường |
| `--color-warning-600` | `#B45309`* | `#FFFBEB` | Chờ duyệt, sắp hết hạn, cần xác nhận | Không trùng vai trò accent trong cùng màn hình |
| `--color-error-600` | `#B91C1C` | `#FEF2F2` | Lỗi, từ chối, hủy, khóa tài khoản | Không dùng để nhấn mạnh chung |
| `--color-info-600` | `#1D4ED8` | `#EFF6FF` | Thông tin trung tính, hướng dẫn, trạng thái đang xử lý | Không dùng thay primary |
| `--color-disabled-bg` | `#F1EFEA` | — | Nền control disabled | Không dùng cho card |

\* `warning` và `accent` dùng chung hue hổ phách nhưng **khác vai trò**: accent = tôn vinh, warning = cảnh báo. Nếu một màn hình có cả hai, accent phải nhường (chuyển sang dạng outline).

### 6.5. Bảng màu trạng thái nghiệp vụ (bắt buộc dùng thống nhất qua `StatusTag`)
| Domain | Giá trị | Màu | Nhãn tiếng Việt |
| :-- | :-- | :-- | :-- |
| TeacherProfile | `DRAFT` | neutral | Bản nháp |
| | `PENDING_APPROVAL` | warning | Chờ duyệt |
| | `APPROVED` | success | Đã duyệt |
| | `REJECTED` | error | Bị từ chối |
| Invoice | `PENDING` | warning | Chờ thanh toán |
| | `PAID` | success | Đã thanh toán |
| | `CANCELLED` | neutral | Đã hủy |
| | `EXPIRED` | error | Hết hạn |
| StudentPackage | `PENDING_PAYMENT` | warning | Chờ thanh toán |
| | `ACTIVE` | success | Đang hoạt động |
| | `COMPLETED` | info | Đã hoàn thành |
| | `LOCKED_EXPIRED` | error | Hết hạn |
| | `REFUND_PENDING` | warning | Đang xử lý hoàn tiền |
| | `REFUNDED` | neutral | Đã hoàn tiền |
| Booking | `SCHEDULED` | info | Đã lên lịch |
| | `COMPLETED` | success | Hoàn thành |
| | `CANCELLED` | neutral | Đã hủy |
| | `EXPIRED` | error | Quá hạn xác nhận |
| Payout | `PENDING` | warning | Chờ duyệt |
| | `PROCESSING` | info | Đang xử lý |
| | `SUCCEEDED` | success | Đã chuyển |
| | `REJECTED` | neutral | Bị từ chối |
| | `FAILED` | error | Thất bại |
| Refund | `PENDING` / `APPROVED` / `PROCESSING` | warning / info / info | Chờ duyệt / Đã duyệt / Đang chuyển |
| | `REFUNDED` / `REJECTED` / `FAILED` | success / neutral / error | Đã hoàn tiền / Bị từ chối / Thất bại |
| Extension | `PENDING` / `APPROVED` / `REJECTED` | warning / success / neutral | Chờ duyệt / Đã duyệt / Bị từ chối |
| Assignment | `DRAFT` / `PUBLISHED` / `CLOSED` | neutral / info / neutral | Bản nháp / Đã giao / Đã đóng |
| Submission | `DRAFT` / `SUBMITTED` / `GRADED` | neutral / info / success | Bản nháp / Đã nộp / Đã chấm |
| Trial | badge riêng | accent outline | Học thử |

### 6.6. Color hierarchy
```text
Mức 1 — Nền:        background (#FBFAF8) → surface (#FFFFFF) → surface-sunken (#F5F3EF)
Mức 2 — Đường nét:  border (#E7E3DC) → border-strong (#D3CEC4)
Mức 3 — Chữ:        text-primary → text-secondary → text-tertiary → text-placeholder
Mức 4 — Hành động:  primary-600 (một hành động chính / màn hình)
Mức 5 — Trạng thái: success | warning | error | info (chỉ khi mang nghĩa)
Mức 6 — Tôn vinh:   accent (tối đa một vùng / màn hình)
```
Không có dark mode trong MVP (**NEEDS CLARIFICATION** nếu cần).

---

## 7. TYPOGRAPHY

### 7.1. Font family
| Vai trò | Font | Lý do | Fallback |
| :-- | :-- | :-- | :-- |
| Heading & UI | **Be Vietnam Pro** (400/500/600/700) | Thiết kế riêng cho tiếng Việt: dấu thanh cân đối, không chồng dấu ở chữ hoa; hình dáng geometric-humanist hiện đại | `"Be Vietnam Pro", "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif` |
| Body & Data | **Inter** (400/500/600) | Đọc tốt ở size nhỏ, có `tabular-nums` cho bảng số liệu | `Inter, "Be Vietnam Pro", system-ui, sans-serif` |
| Mono (mã giao dịch, order code, JSON audit) | **JetBrains Mono** (400/500) | Phân biệt rõ 0/O, 1/l | `"JetBrains Mono", "SF Mono", Consolas, monospace` |

Chỉ 2 font hiển thị + 1 mono kỹ thuật. Load subset `latin` + `vietnamese`, `font-display: swap`, preconnect Google Fonts.

### 7.2. Type scale
| Token | Dùng cho | Size | Weight | Line-height | Letter-spacing | Font |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| `--text-display` | Hero landing | 48px (mobile 32px) | 700 | 1.15 | -0.02em | Heading |
| `--text-h1` | Tiêu đề trang | 32px (mobile 24px) | 700 | 1.25 | -0.015em | Heading |
| `--text-h2` | Tiêu đề section | 24px (mobile 20px) | 600 | 1.3 | -0.01em | Heading |
| `--text-h3` | Tiêu đề card lớn | 20px (mobile 18px) | 600 | 1.35 | -0.005em | Heading |
| `--text-h4` | Tiêu đề card nhỏ, modal title | 16px | 600 | 1.4 | 0 | Heading |
| `--text-body-lg` | Đoạn dẫn, mô tả hero | 18px | 400 | 1.6 | 0 | Body |
| `--text-body` | Nội dung mặc định | 15px | 400 | 1.6 | 0 | Body |
| `--text-body-sm` | Nội dung phụ, bảng | 14px | 400 | 1.55 | 0 | Body |
| `--text-caption` | Timestamp, ghi chú, helper text | 12px | 400 | 1.45 | 0.005em | Body |
| `--text-button` | Chữ trong nút | 14px (lg 15px, sm 13px) | 600 | 1 | 0.01em | Heading |
| `--text-label` | Label form | 13px | 500 | 1.4 | 0.005em | Body |
| `--text-nav` | Menu sidebar & navbar | 14px | 500 | 1.4 | 0 | Heading |
| `--text-overline` | Group label sidebar, eyebrow | 11px | 600 | 1.3 | 0.06em, UPPERCASE | Heading |
| `--text-mono` | Mã giao dịch, order code | 13px | 400 | 1.5 | 0 | Mono |

### 7.3. Quy tắc riêng cho số
- Mọi số tiền, số buổi, số liệu bảng: `font-variant-numeric: tabular-nums;`
- Số tiền lớn trong StatsCard: 28px / weight 700 / `--color-text-primary`; đơn vị `₫` 16px / weight 500 / `--color-text-secondary`.
- Số tiền dương trong ledger: `--color-success-600`, prefix `+`. Số âm: `--color-error-600`, prefix `−` (U+2212, không dùng hyphen).
- Không bao giờ để số tiền wrap xuống dòng (`white-space: nowrap`).

### 7.4. Quy tắc chung
- Tối đa 3 mức chữ khác nhau trong một card.
- Độ dài dòng tối ưu: 60–75 ký tự (`max-width: 68ch`) cho đoạn văn dài (bio giáo viên, session report).
- Không dùng chữ in hoa toàn bộ ngoài `--text-overline`.
- Không dùng `font-weight` 300 (dấu tiếng Việt bị mảnh, khó đọc).

---

## 8. SPACING SYSTEM

### 8.1. Thang spacing (base 4px)
| Token | px | Dùng cho |
| :-- | :-- | :-- |
| `--space-1` | 4 | Gap icon–text nhỏ, khoảng cách badge |
| `--space-2` | 8 | Gap trong control, gap giữa tag |
| `--space-3` | 12 | Padding ngang input nhỏ, gap list item |
| `--space-4` | 16 | Padding card mobile, gap grid mobile, khoảng cách field |
| `--space-5` | 20 | Padding card mặc định (mobile), gap giữa nhóm |
| `--space-6` | 24 | **Padding card desktop**, gap grid desktop, page padding mobile |
| `--space-8` | 32 | Page padding desktop, khoảng cách giữa các block trong page |
| `--space-10` | 40 | Khoảng cách section trong dashboard |
| `--space-12` | 48 | Khoảng cách section trang public |
| `--space-16` | 64 | Padding dọc section landing (desktop) |
| `--space-20` | 80 | Padding dọc hero landing |

### 8.2. Quy tắc áp dụng (bắt buộc, không tùy biến theo page)
| Ngữ cảnh | Desktop | Tablet | Mobile |
| :-- | :-- | :-- | :-- |
| Page padding ngang | 32px | 24px | 16px |
| Page padding trên (dưới header) | 24px | 24px | 16px |
| Khoảng cách giữa Page Header và nội dung | 24px | 24px | 20px |
| Section spacing (trong dashboard) | 40px | 32px | 32px |
| Section spacing (landing public) | 96px | 64px | 48px |
| Card padding | 24px | 20px | 16px |
| Card padding (card dày dữ liệu, ví dụ ledger) | 20px | 16px | 16px |
| Grid gap | 24px | 20px | 16px |
| Khoảng cách giữa 2 field form (dọc) | 20px | 20px | 20px |
| Khoảng cách label → input | 6px | | |
| Khoảng cách input → helper/error | 6px | | |
| Gap giữa các nút trong nhóm | 12px | 12px | 8px |
| Gap giữa icon và text trong nút | 8px | | |
| Padding trong table cell | 12px 16px | 12px 16px | 12px |
| Khoảng cách giữa các nhóm field (fieldset) | 32px | | 24px |
| Sticky action bar padding | 16px 32px | 16px 24px | 12px 16px |

**Cấm**: giá trị spacing ngoài thang (13px, 18px, 30px…). Ngoại lệ duy nhất: căn chỉnh quang học icon (±1px).

---

## 9. LAYOUT SYSTEM

### 9.1. Breakpoints
| Tên | Range | Token |
| :-- | :-- | :-- |
| Mobile | < 640px | `--bp-sm: 640px` |
| Tablet | 640px – 1023px | `--bp-md: 768px`, `--bp-lg: 1024px` |
| Desktop | 1024px – 1439px | `--bp-xl: 1280px` |
| Wide | ≥ 1440px | `--bp-2xl: 1440px` |

### 9.2. Layout khu vực public
```text
┌──────────────────────────────────────────────┐
│ Navbar (sticky, h=64)                        │
├──────────────────────────────────────────────┤
│        Container max-width 1200px            │
│        padding ngang 32 / 24 / 16            │
│        (trang tìm kiếm: max-width 1320px)    │
├──────────────────────────────────────────────┤
│ Footer (nền primary-900, padding dọc 64)     │
└──────────────────────────────────────────────┘
```

### 9.3. Layout khu vực app (Student / Teacher / Admin)
```text
Desktop ≥1024px
┌──────────┬───────────────────────────────────┐
│ Sidebar  │ Header (h=64, sticky)             │
│ 248px    ├───────────────────────────────────┤
│ fixed    │ Content                           │
│ scroll   │   max-width 1280px, căn trái      │
│ độc lập  │   padding 24px 32px 48px          │
└──────────┴───────────────────────────────────┘
```
- Sidebar: `position: fixed; width: 248px;` content dùng `margin-left: 248px`.
- Trang bảng dữ liệu Admin: content `max-width: none` (full width) để bảng thở.
- Trang form/chi tiết: nội dung `max-width: 880px` để không kéo dài dòng.
- Trang chat: full height, không scroll trang (`height: calc(100vh - 64px)`), scroll nội bộ 2 cột.

### 9.4. Grid system
- 12 cột, gap 24px (desktop) / 20px (tablet) / 16px (mobile).
- Card grid tiêu chuẩn:
  | Ngữ cảnh | Desktop | Tablet | Mobile |
  | :-- | :-- | :-- | :-- |
  | StatsCard dashboard | 4 cột | 2 cột | 1 cột (hoặc 2 cột nếu nội dung ngắn) |
  | TeacherCard (tìm kiếm) | 3 cột | 2 cột | 1 cột |
  | PackageCard | 3 cột | 2 cột | 1 cột |
  | SubjectCard | 4 cột | 3 cột | 2 cột |
  | Form 2 cột | 2 cột | 2 cột | 1 cột |
- Trang tìm kiếm giáo viên: sidebar filter 280px cố định + kết quả `1fr` (desktop); tablet/mobile → filter chuyển thành nút "Bộ lọc" mở Drawer.

### 9.5. Hành vi theo breakpoint
| Thành phần | Desktop | Tablet | Mobile |
| :-- | :-- | :-- | :-- |
| Sidebar | Hiện, 248px | Thu gọn 72px (icon) hoặc overlay drawer | Ẩn, mở bằng hamburger drawer |
| Header breadcrumb | Hiện | Hiện | Thay bằng tiêu đề trang + nút back |
| Bottom nav | Không | Không | Có (Student/Teacher) |
| Table | Bảng đầy đủ | Bảng + horizontal scroll | Chuyển thành card list |
| Filter panel | Sidebar cố định | Drawer | Drawer full-height |
| Modal | Width cố định (480/640/800) | 90vw | Full-screen sheet, trượt từ dưới |
| Calendar tuần | Lưới 7 ngày | Lưới 7 ngày (scroll ngang) | Danh sách theo ngày (agenda view) |
| Chat | 2 cột (list 320px + thread) | 2 cột (list 280px) | 1 cột, list → thread là 2 màn hình |
| Form action | Inline dưới form | Inline | Sticky bottom bar |

---

## 10. DESIGN TOKENS

### 10.1. Naming convention
`--{category}-{name}-{variant?}` — category ∈ {color, text, font, space, radius, shadow, border, z, size, duration, ease, bp}.

### 10.2. Bảng token
```css
:root {
  /* ---- Color: brand ---- */
  --color-primary-50:  #F0FDFA;
  --color-primary-100: #CCFBF1;
  --color-primary-500: #14B8A6;
  --color-primary-600: #0F766E;
  --color-primary-700: #115E59;
  --color-primary-900: #042F2E;
  --color-accent-50:   #FFFBEB;
  --color-accent-500:  #B45309;

  /* ---- Color: surface & text ---- */
  --color-background:      #FBFAF8;
  --color-surface:         #FFFFFF;
  --color-surface-sunken:  #F5F3EF;
  --color-surface-hover:   #F7F6F3;
  --color-border:          #E7E3DC;
  --color-border-strong:   #D3CEC4;
  --color-text-primary:    #1C1917;
  --color-text-secondary:  #57534E;
  --color-text-tertiary:   #8A837B;
  --color-text-placeholder:#A8A29E;
  --color-text-inverse:    #FFFFFF;
  --color-text-disabled:   #B9B3AB;
  --color-disabled-bg:     #F1EFEA;

  /* ---- Color: semantic ---- */
  --color-success-600: #15803D;  --color-success-bg: #F0FDF4;
  --color-warning-600: #B45309;  --color-warning-bg: #FFFBEB;
  --color-error-600:   #B91C1C;  --color-error-bg:   #FEF2F2;
  --color-info-600:    #1D4ED8;  --color-info-bg:    #EFF6FF;

  /* ---- Typography ---- */
  --font-heading: "Be Vietnam Pro", "Segoe UI", Roboto, Arial, sans-serif;
  --font-body:    Inter, "Be Vietnam Pro", system-ui, sans-serif;
  --font-mono:    "JetBrains Mono", "SF Mono", Consolas, monospace;
  --text-display: 48px; --text-h1: 32px; --text-h2: 24px; --text-h3: 20px; --text-h4: 16px;
  --text-body-lg: 18px; --text-body: 15px; --text-body-sm: 14px; --text-caption: 12px;
  --text-button: 14px; --text-label: 13px; --text-nav: 14px; --text-overline: 11px;
  --weight-regular: 400; --weight-medium: 500; --weight-semibold: 600; --weight-bold: 700;

  /* ---- Spacing ---- */
  --space-1: 4px;  --space-2: 8px;  --space-3: 12px; --space-4: 16px; --space-5: 20px;
  --space-6: 24px; --space-8: 32px; --space-10: 40px; --space-12: 48px;
  --space-16: 64px; --space-20: 80px;

  /* ---- Radius ---- */
  --radius-xs: 4px;   /* tag, badge nhỏ */
  --radius-sm: 6px;   /* input nhỏ, checkbox */
  --radius-md: 8px;   /* button, input, select */
  --radius-lg: 12px;  /* card, alert */
  --radius-xl: 16px;  /* modal, drawer, card lớn */
  --radius-full: 9999px; /* avatar, pill, badge tròn */

  /* ---- Shadow ---- */
  --shadow-xs: 0 1px 2px rgba(28,25,23,.04);
  --shadow-sm: 0 1px 3px rgba(28,25,23,.06), 0 1px 2px rgba(28,25,23,.04);
  --shadow-md: 0 4px 12px rgba(28,25,23,.08);
  --shadow-lg: 0 12px 32px rgba(28,25,23,.12);
  --shadow-focus: 0 0 0 3px rgba(20,184,166,.28);

  /* ---- Border ---- */
  --border-width: 1px;
  --border-default: 1px solid var(--color-border);
  --border-strong:  1px solid var(--color-border-strong);

  /* ---- Breakpoints ---- */
  --bp-sm: 640px; --bp-md: 768px; --bp-lg: 1024px; --bp-xl: 1280px; --bp-2xl: 1440px;

  /* ---- Z-index ---- */
  --z-base: 0; --z-sticky: 100; --z-header: 200; --z-sidebar: 300;
  --z-dropdown: 400; --z-overlay: 500; --z-modal: 600; --z-drawer: 600;
  --z-toast: 700; --z-tooltip: 800;

  /* ---- Component heights ---- */
  --size-control-sm: 32px; --size-control-md: 40px; --size-control-lg: 48px;
  --size-header: 64px; --size-sidebar: 248px; --size-sidebar-collapsed: 72px;
  --size-bottomnav: 56px; --size-filter-panel: 280px;
  --size-avatar-xs: 24px; --size-avatar-sm: 32px; --size-avatar-md: 40px;
  --size-avatar-lg: 64px; --size-avatar-xl: 96px;
  --size-icon-sm: 16px; --size-icon-md: 18px; --size-icon-lg: 20px; --size-icon-xl: 24px;
  --size-container: 1200px; --size-container-wide: 1320px; --size-content: 1280px;
  --size-form: 880px;

  /* ---- Motion ---- */
  --duration-instant: 80ms; --duration-fast: 120ms;
  --duration-base: 180ms;   --duration-slow: 240ms;
  --ease-standard: cubic-bezier(.2,0,0,1);
  --ease-out: cubic-bezier(0,0,.2,1);
  --ease-in: cubic-bezier(.4,0,1,1);
}
```

### 10.3. Ánh xạ sang Ant Design theme
```ts
theme: {
  token: {
    colorPrimary: '#0F766E', colorSuccess: '#15803D', colorWarning: '#B45309',
    colorError: '#B91C1C', colorInfo: '#1D4ED8',
    colorBgLayout: '#FBFAF8', colorBgContainer: '#FFFFFF', colorBorder: '#E7E3DC',
    colorText: '#1C1917', colorTextSecondary: '#57534E', colorTextTertiary: '#8A837B',
    colorTextPlaceholder: '#A8A29E',
    borderRadius: 8, borderRadiusLG: 12, borderRadiusSM: 6,
    fontFamily: 'Inter, "Be Vietnam Pro", system-ui, sans-serif',
    fontSize: 15, controlHeight: 40, controlHeightSM: 32, controlHeightLG: 48,
    motionDurationMid: '0.18s',
  },
  components: {
    Button: { fontWeight: 600, primaryShadow: 'none' },
    Card:   { paddingLG: 24, headerFontSize: 16 },
    Table:  { headerBg: '#F5F3EF', headerColor: '#57534E', rowHoverBg: '#F7F6F3', cellPaddingBlock: 12 },
    Layout: { siderBg: '#FFFFFF', headerBg: '#FFFFFF', headerHeight: 64 },
    Menu:   { itemHeight: 40, itemBorderRadius: 8, itemSelectedBg: '#F0FDFA', itemSelectedColor: '#0F766E' },
    Modal:  { borderRadiusLG: 16 },
  }
}
```

---

# PHỤ LỤC KỸ THUẬT — ÁP DỤNG NEXT.JS APP ROUTER

> Phụ lục này **chỉ thay thế kế hoạch triển khai frontend** từ React 18 + Vite + React Router sang Next.js App Router. Toàn bộ UX/UI, business logic, role, quyền, trạng thái, nội dung tiếng Việt, page inventory, component inventory, design token và responsive behavior ở phần spec gốc phía trên được giữ nguyên.

## A. Phạm vi migration

| Hạng mục cũ | Hạng mục áp dụng | Ghi chú |
| :-- | :-- | :-- |
| React 18 SPA | Next.js App Router | Dùng React Server Components mặc định; Client Components tại boundary có tương tác |
| Vite | Next.js build/runtime | Không dùng `VITE_*`; chuyển sang quy ước biến môi trường của Next.js |
| React Router | File-system routing | Dùng `app/`, route groups, nested layouts, dynamic segments, `Link`, `redirect`, `notFound` |
| Client-only boot | Server render + hydration | Chọn SSR/ISR/dynamic theo tính chất dữ liệu |
| Guard trong router client | Middleware + server guard + client capability guard | Backend vẫn là lớp authorization cuối cùng |
| Google Fonts runtime | `next/font` | Font được self-host trong build output |

Không thay đổi endpoint backend hoặc payload nghiệp vụ chỉ vì migration. Nếu contract API cần sửa, phải được duyệt như một thay đổi riêng.

## B. Kiến trúc ứng dụng

### B.1. Nguyên tắc Server/Client Component

- Mọi `page.tsx` và `layout.tsx` là Server Component mặc định.
- Chỉ thêm `'use client'` cho phần cần state browser, event handler, Ant Design interactive API, React Hook Form, TanStack Query hook hoặc STOMP.
- Giữ client boundary nhỏ: page server fetch dữ liệu ban đầu rồi truyền dữ liệu serializable xuống interactive island.
- Module đọc cookie, secret, internal API URL hoặc refresh token phải dùng `server-only` và không được import vào client bundle.
- Route file chỉ compose màn hình; business logic nằm trong `features/` và `lib/`.

### B.2. Providers

`src/app/providers.tsx` là Client Component và chứa:

- `ConfigProvider` của Ant Design.
- `AntdRegistry`/registry tương thích App Router để thu thập CSS khi SSR.
- `QueryClientProvider` của TanStack Query.
- notification/message holder dùng chung.
- `RealtimeProvider` chỉ khởi tạo sau khi có session hợp lệ.

Root layout chịu trách nhiệm metadata, font variables, locale `vi-VN`, timezone hiển thị và providers. Không đặt toàn bộ app thành Client Component.

## C. Routing và layouts

### C.1. Cấu trúc route groups

```text
src/app/
├── (public)/
│   ├── layout.tsx
│   ├── page.tsx
│   ├── subjects/page.tsx
│   ├── teachers/page.tsx
│   ├── teachers/[id]/page.tsx
│   └── ranking/page.tsx
├── (auth)/auth/
│   ├── layout.tsx
│   ├── login/page.tsx
│   ├── register/page.tsx
│   ├── oauth/role/page.tsx
│   ├── forgot-password/page.tsx
│   └── reset-password/page.tsx
├── (student)/student/
│   ├── layout.tsx
│   ├── page.tsx
│   ├── packages/[id]/page.tsx
│   ├── checkout/[invoiceId]/page.tsx
│   ├── payment-result/[invoiceId]/page.tsx
│   ├── bookings/[id]/page.tsx
│   ├── assignments/[id]/page.tsx
│   └── messages/[[...conversationId]]/page.tsx
├── (teacher)/teacher/
│   ├── layout.tsx
│   ├── page.tsx
│   ├── packages/[id]/edit/page.tsx
│   ├── bookings/[id]/page.tsx
│   ├── students/[id]/page.tsx
│   ├── assignments/[id]/page.tsx
│   ├── submissions/[id]/page.tsx
│   └── messages/[[...conversationId]]/page.tsx
├── (admin)/admin/
│   ├── layout.tsx
│   ├── page.tsx
│   ├── teacher-approvals/[id]/page.tsx
│   ├── subject-proposals/[id]/page.tsx
│   ├── users/[id]/page.tsx
│   ├── refunds/[id]/page.tsx
│   ├── extensions/[id]/page.tsx
│   └── payouts/[id]/page.tsx
├── api/
├── 403/page.tsx
├── error.tsx
├── global-error.tsx
├── loading.tsx
├── not-found.tsx
├── layout.tsx
└── providers.tsx
```

Các route tĩnh còn lại trong cây trang của spec gốc được tạo đúng dưới zone tương ứng. Route groups không làm thay đổi URL hiển thị.

### C.2. Mapping cú pháp route

- `:id` trong spec tương ứng `[id]` trong App Router.
- `messages(/:conversationId)` tương ứng `messages/[[...conversationId]]` hoặc hai page `messages/page.tsx` và `messages/[conversationId]/page.tsx`.
- Dùng `next/link` cho link, `useRouter` cho thao tác client, `redirect()` cho redirect server, `notFound()` cho tài nguyên không tồn tại.
- Search/filter/sort/pagination được lưu trong URL search params để refresh, chia sẻ link và Back/Forward không mất trạng thái.

## D. Route guard, auth và session

### D.1. Mô hình bảo vệ nhiều lớp

1. `middleware.ts` kiểm tra sự hiện diện của session cookie và redirect nhanh Guest khỏi `/student`, `/teacher`, `/admin`.
2. Protected layout/page gọi `requireUser()` và `requireRole()` phía server trước khi render dữ liệu.
3. Teacher page nhạy cảm gọi thêm `requireTeacherApproved()`; checkout/submit profile gọi `requireEmailVerified()`.
4. Client capability guard chỉ dùng để ẩn/disable menu, button, tooltip và banner.
5. Backend luôn kiểm tra role, ownership và trạng thái nghiệp vụ cho mọi request.

Middleware không thực hiện fetch nặng hoặc thay backend authorization. Trường hợp resource cần tránh lộ sự tồn tại có thể dùng `notFound()`; các trường hợp role sai theo UX đã chốt chuyển `/403`.

### D.2. Session

- Session dùng cookie `HttpOnly`, `Secure`, `SameSite` phù hợp; không lưu access/refresh token trong `localStorage`, `sessionStorage`, URL hoặc state lâu sống.
- `getCurrentUser()` chạy server-side. Dữ liệu hydrate xuống client chỉ gồm trường tối thiểu: id, role, status, email verification, teacher approval và display info.
- Redirect sau login chỉ chấp nhận path nội bộ bắt đầu bằng `/`; loại bỏ URL ngoài domain để tránh open redirect.
- Auth routes redirect người đã đăng nhập về dashboard đúng role.
- Google OAuth2 callback đặt ở route handler hoặc backend callback đã thống nhất; sau callback thiếu role chuyển `/auth/oauth/role`.

### D.3. Refresh session

HTTP adapter chỉ cho phép một refresh request tại một thời điểm và xếp hàng các request 401 còn lại. Refresh thất bại phải xóa session, clear user-scoped query cache, chuyển `/auth/login?redirect=<safe-path>` và hiện toast `Phiên đăng nhập đã hết hạn` đúng spec gốc.

Trong lần kiểm tra session đầu tiên, protected layout dùng server render/skeleton phù hợp để không nháy sai layout hoặc menu role.

## E. Data fetching, cache và mutation

### E.1. Server data

- Public data dùng server fetch; đặt `revalidate` theo độ mới cần thiết.
- Dữ liệu phụ thuộc user/session dùng `cache: 'no-store'` hoặc cơ chế cache tách biệt theo user; không cache chung response riêng tư.
- Server API client chịu trách nhiệm base URL, timeout, auth cookie/header, correlation id và chuẩn hóa lỗi.
- Không để page/component tự ghép base URL hoặc tự xử lý token.

### E.2. TanStack Query

TanStack Query tiếp tục được dùng cho dữ liệu tương tác, mutation, polling và realtime reconciliation. Query key chuẩn theo domain, ví dụ:

```ts
['teachers', normalizedSearchParams]
['teacher', teacherId]
['student-package', packageId]
['bookings', filters]
['conversation', conversationId]
['notifications', userId]
```

Server có thể prefetch và hydrate query cần thiết. Sau mutation dùng `invalidateQueries`, `setQueryData` hoặc optimistic update có rollback; không reload toàn trang.

### E.3. URL state

Teacher search, Admin filter, sort, page và tab dùng `searchParams`. Giá trị phải được validate và normalize phía server. Client filter dùng `router.replace(..., { scroll: false })`; debounce text search nhưng không debounce select trạng thái.

### E.4. Mutation nghiệp vụ

Booking, checkout, complete/cancel, assignment, submission, review, refund, extension, bank account và payout phải có loading/disabled state, idempotency key khi backend hỗ trợ, toast thành công và mapping lỗi theo code. HTTP 409 optimistic locking hiện dialog tải lại; `BOOKING_TIME_CONFLICT` highlight trường giờ và liệt kê booking trùng; cảnh báo ngoài availability vẫn cho submit đúng spec.

### E.5. payOS

`/student/checkout/[invoiceId]` polling trạng thái invoice mỗi 3 giây, tối đa 5 phút. `returnUrl` không phải bằng chứng thanh toán; chỉ webhook-confirmed status `PAID` mới dẫn tới success. Dừng polling khi status terminal, component unmount hoặc hết thời gian; khi tab hidden có thể tạm dừng và refetch ngay lúc visible.

## F. Rendering strategy

| Khu vực | Chiến lược |
| :-- | :-- |
| Landing, subjects, ranking | SSR/ISR theo freshness |
| Teacher search và public profile | SSR theo search params; hydrate filter tương tác |
| Auth pages/callback | Dynamic, `no-store` |
| Student/Teacher/Admin dashboard | Dynamic SSR + client query hydration |
| Checkout/payment result | Dynamic `no-store` + client polling |
| Chat/notification | Server snapshot + Client Component realtime |
| Calendar, form, data table | Server initial data + client interactive island |

Không dùng static caching cho dữ liệu riêng tư. Metadata/SEO chỉ ưu tiên public pages; app pages có thể `noindex` theo chính sách sản phẩm.

## G. Ant Design và design tokens

- Giữ nguyên toàn bộ token màu, typography, spacing, radius, shadow, breakpoint, z-index và component size ở mục 6–10.
- `ConfigProvider` ánh xạ đúng token đã chốt; không thay màu/spacing mặc định làm lệch visual spec.
- Dùng registry tương thích Next.js App Router để style Ant Design xuất hiện trong SSR và tránh flash/hydration mismatch.
- Shell có thể dùng `Layout`, `Sider`, `Header`, `Content`, `Menu`, `Drawer`, `Breadcrumb`; page content vẫn ưu tiên Server Component.
- Notification/message API dùng holder trong provider, không gọi từ Server Component.
- CSS token chuẩn nằm ở `src/app/globals.css`; style riêng theo component dùng CSS Modules hoặc giải pháp đã thống nhất, không tạo token trùng theo page.

## H. Font loading

Dùng `next/font/google` trong root layout để self-host font:

```ts
import { Be_Vietnam_Pro, Inter, JetBrains_Mono } from 'next/font/google';

const headingFont = Be_Vietnam_Pro({
  subsets: ['latin', 'vietnamese'],
  weight: ['400', '500', '600', '700'],
  variable: '--font-heading',
  display: 'swap',
});

const bodyFont = Inter({
  subsets: ['latin', 'vietnamese'],
  weight: ['400', '500', '600'],
  variable: '--font-body',
  display: 'swap',
});

const monoFont = JetBrains_Mono({
  subsets: ['latin'],
  weight: ['400', '500'],
  variable: '--font-mono',
  display: 'swap',
});
```

Gắn ba font variable vào `<body>`. Không dùng `@import` hoặc runtime request tới Google Fonts.

## I. Realtime

- STOMP.js chỉ chạy trong Client Component sau khi browser mount và session hợp lệ.
- Tạo một adapter `lib/realtime` quản lý connect, subscribe, unsubscribe, heartbeat, cleanup và exponential backoff.
- Subscribe theo user/conversation; event mới cập nhật TanStack Query cache và unread count.
- Optimistic message cần client-generated id để reconcile với event server, tránh render trùng.
- Mất kết nối: header hiện `Đang kết nối lại…`, chat read-only; reconnect thành công refetch conversation và unread count.
- Không đưa token vào WebSocket URL nếu có thể dùng cookie hoặc cơ chế handshake an toàn.
- Relative timestamp phải tránh hydration mismatch: render thời gian tuyệt đối ở server hoặc format relative sau mount.

## J. Environment và config

```text
NEXT_PUBLIC_APP_URL=
NEXT_PUBLIC_API_BASE_URL=
NEXT_PUBLIC_WS_URL=
NEXT_PUBLIC_PAYOS_CLIENT_URL=
API_INTERNAL_URL=
AUTH_SESSION_SECRET=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
```

Chỉ biến `NEXT_PUBLIC_*` được bundle vào browser. Secret, internal URL, refresh/session secret và OAuth secret chỉ đọc phía server. Không commit `.env*`, không log token/secret. Vì public env thường được đóng vào build output, thay đổi production public config cần build lại hoặc dùng endpoint runtime config công khai có allowlist.

Production phải dùng HTTPS, Secure cookie, CSP phù hợp, trusted origins, CORS/cookie domain/WebSocket origin theo environment và source-map policy không lộ thông tin nhạy cảm.

## K. Folder structure

```text
src/
├── app/                             # route, layout, loading, error, route handlers
├── components/
│   ├── ui/                          # component dùng chung theo design system
│   ├── layout/                      # navbar, shell, sidebar, mobile nav
│   ├── marketplace/
│   ├── learning/
│   ├── finance/
│   └── realtime/
├── features/
│   ├── auth/
│   ├── teachers/
│   ├── packages/
│   ├── bookings/
│   ├── assignments/
│   ├── finance/
│   ├── admin/
│   └── notifications/
├── lib/
│   ├── api/
│   ├── auth/
│   ├── query/
│   ├── realtime/
│   ├── format/
│   └── validation/
├── types/
└── middleware.ts
```

`components/ui` không biết role cụ thể; feature chứa logic theo domain; `lib` chứa adapter/hạ tầng; API/domain types được sinh từ OpenAPI nếu backend có schema ổn định.

## L. Loading, error, form và accessibility

- `loading.tsx` cho route-level skeleton; `Suspense` cho vùng độc lập; `Skeleton`, `Spin` và loading state của Table cho component-level.
- `error.tsx` có nút thử lại; không lộ stack, token hoặc raw backend response.
- `not-found.tsx` cho tài nguyên không tồn tại; `/403` cho role không được phép.
- React Hook Form giữ validation/copy hiện tại; schema validator đồng bộ contract backend.
- Error field map theo backend code; form dài có error summary; focus field lỗi đầu tiên.
- Empty state phân biệt chưa có dữ liệu và không có kết quả lọc.
- Giữ WCAG, keyboard, focus, contrast, reduced motion, semantic heading và ARIA theo spec gốc.

## M. Implementation plan

### Phase 0 — Foundation

Khởi tạo Next.js App Router + TypeScript strict; cài Ant Design registry, TanStack Query, Axios, STOMP.js, React Hook Form và validator; thiết lập font, globals, theme, providers, error/loading/not-found, lint, typecheck và test.

### Phase 1 — Design system và layouts

Migrate nguyên vẹn design token/component system; dựng public/auth/student/teacher/admin layouts, sidebar, header, bottom nav, drawer, breadcrumb và responsive behavior; đối chiếu visual với spec gốc.

### Phase 2 — Public và auth

Migrate landing, subjects, teachers, teacher profile, ranking; hoàn thiện session, login/register, forgot/reset, email verification, Google OAuth2, role selection, middleware và server guards.

### Phase 3 — Student

Migrate dashboard, package, checkout QR, webhook polling, payment result, bookings, trial, refund, extension, requests, assignments, messages, notifications và profile.

### Phase 4 — Teacher

Migrate profile/documents/subjects/proposals, approval guard/banner, packages, availability, schedule, bookings, students, assignments/submissions, wallet, bank accounts, payouts, stats, messages và notifications.

### Phase 5 — Admin và realtime

Migrate dashboard Admin, approvals, subjects, users, refund/extension/payout, audit/settings; hoàn thiện STOMP reconnect, cache reconciliation, unread và read-only fallback.

### Phase 6 — Hardening và release

Kiểm thử role/ownership, responsive, accessibility, timezone/currency, payment edge cases, 401/403/409, reconnect, upload, hydration và cache riêng tư; chạy E2E, Lighthouse public pages, bundle check; deploy staging trước production.

## N. Definition of Done

- [ ] Toàn bộ nội dung UI/UX và business logic của spec gốc được triển khai, không bị cắt hoặc đổi nghĩa.
- [ ] Không còn dependency `react-router-dom`, Vite config hoặc `VITE_*`.
- [ ] Dynamic route, route groups và nested layouts đúng App Router.
- [ ] Protected zone có middleware, server guard và backend authorization.
- [ ] Token không nằm trong localStorage/sessionStorage/URL; session dùng HttpOnly cookie.
- [ ] Public/private caching đúng; dữ liệu riêng tư không cache chung.
- [ ] Ant Design SSR không flash style hoặc hydration warning.
- [ ] Font tải qua `next/font`; tiếng Việt hiển thị đúng weight/subset.
- [ ] payOS chỉ xác nhận thành công sau webhook-confirmed `PAID`.
- [ ] STOMP cleanup/reconnect, unread count và read-only fallback hoạt động.
- [ ] Search/filter/pagination giữ URL state.
- [ ] Typecheck, lint, unit/integration/E2E và accessibility checks đạt trong CI.

