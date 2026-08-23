# Frontend Implementation Plan

Kế hoạch triển khai Frontend trong 8 tuần cho hai thành viên Full-stack, tổ chức theo feature để tránh deep import và sửa chồng file.

## 1. Phân chia ownership

### Thành viên A — Identity & Experience

Sở hữu các feature:

- `auth`
- `teacher-profile`
- `marketplace`
- `catalog`
- `learning`
- `chat`
- `notifications`
- `ranking`
- Shared design system trong `shared/components` và `shared/lib`.

### Thành viên B — Transaction & Operations

Sở hữu các feature:

- `payments`
- `student-packages`
- `bookings`
- `finance`
- `admin`
- App shell, router/providers trong `app`.
- Axios client, response/error mapping trong `shared/api`.

### Quy tắc chống giẫm chân

- Mỗi feature tự chứa `api`, `components`, `hooks`, `pages`, `schemas` và `types`.
- Không deep import từ feature khác; chỉ import qua public `index.ts`.
- A không sửa `app/router` hoặc `shared/api`; B không sửa internals của feature do A sở hữu.
- Thay đổi shared component, router hoặc API client phải nằm trong PR riêng.
- Mỗi route/feature dùng branch ngắn, ví dụ `feat/teacher-onboarding-ui`, `feat/payment-checkout-ui`.
- Người sở hữu feature chịu trách nhiệm UI, API hooks, validation, loading/error/empty state và test.
- FE không tự suy luận business state; mọi trạng thái thanh toán, booking và quyền truy cập lấy từ Backend.

## 2. Kế hoạch 8 tuần

### Tuần 1 — Frontend Foundation

**B**

- Thiết lập router, providers, Axios và TanStack Query.
- Refresh-token queue và retry request sau refresh thành công.
- Route guard theo authentication, role và Teacher approval.
- Chuẩn hóa mapping response/error theo `ERROR_CODES.md`.

**A**

- Xây dựng design tokens và responsive application layout.
- Shared form controls, loading, empty và error states.
- Shared table, pagination, upload và confirmation components.

**Checkpoint:** app shell chạy được với mock session, route guard và error boundary.

### Tuần 2 — Auth và Onboarding

**A**

- Login/register và chọn role Student/Teacher.
- Email verification, forgot/reset password và OAuth completion.
- Teacher profile wizard và trạng thái approval.
- Upload chứng chỉ, chọn/đề xuất môn học.
- Student parent-contact form.

**B**

- Admin Teacher approval queue/detail.
- Admin SubjectProposal queue/detail.
- User moderation screen.

**Checkpoint:** chạy được UI flow Teacher đăng ký → gửi hồ sơ → Admin duyệt.

### Tuần 3 — Marketplace

**A**

- Landing page và subject list.
- Teacher search/filter, pagination và URL query state.
- Teacher detail, availability, packages và reviews.
- Global ranking.
- Teacher profile, availability và PricingPackage management.

**B**

- Student dashboard shell.
- Package summary placeholder qua public export, không import internals của Marketplace.

**Checkpoint:** Student tìm Teacher theo môn, giá, rating, delivery mode và giờ rảnh.

### Tuần 4 — Checkout và StudentPackage

**B**

- payOS checkout flow.
- Result/cancel pages và Invoice polling.
- Không coi redirect query parameter là bằng chứng thanh toán thành công.
- StudentPackage list/detail và session counters.

**A**

- Notification UX cho payment success/failure.
- Marketplace CTA chuyển sang checkout qua public feature interface.

**Checkpoint:** UI cập nhật Invoice/StudentPackage từ trạng thái Backend sau webhook.

### Tuần 5 — Booking

**B**

- Student và Teacher calendar.
- Teacher tạo/hủy/hoàn thành Booking.
- SessionReport form và detail view.
- Trial request/accept/reject.
- Student xem lịch, trạng thái buổi học và SessionReport.

**A**

- Review form chỉ hiển thị cho Booking đủ điều kiện.
- Notification UX cho Booking, reminder và expiration.

**Checkpoint:** tạo Booking, chống submit lặp và cập nhật counters sau state transition.

### Tuần 6 — Learning và Chat

**A**

- Assignment builder và content blocks.
- Submission form, grading và feedback.
- Attachment uploader/viewer.
- Conversation list, message history và realtime STOMP chat.
- Notification center, unread badge và read/read-all.

**B**

- Cung cấp route context và booking links qua public interfaces.
- Không sửa internals của Learning, Chat hoặc Notifications.

**Checkpoint:** hoàn chỉnh giao/nộp/chấm bài; realtime chat reconnect không gửi trùng message.

### Tuần 7 — Finance, Ranking và Dashboard

**B**

- Teacher Wallet/Ledger.
- Bank account và payout request.
- Student refund/extension request.
- Admin finance queues, settings, audit logs và dashboard charts.

**A**

- Teacher statistics/ranking dashboard.
- Public global ranking hoàn chỉnh.

**Checkpoint:** finance forms dùng version hiện tại, xử lý conflict và không tự cập nhật balance trước response Backend.

### Tuần 8 — Hardening

**A**

- Component/hook tests cho Auth, Marketplace, Learning, Chat và Ranking.
- Accessibility và responsive QA cho shared components.

**B**

- Component/hook tests cho Payment, Packages, Booking, Finance và Admin.
- Kiểm tra refresh token, expired session và forbidden routes.

**Cả hai**

- E2E onboarding, marketplace, checkout, booking, learning/chat và finance.
- Kiểm tra loading, empty, error và retry states.
- Production build phải sạch TypeScript và lint errors.

## 3. Route ownership đề xuất

| Route group | Owner |
|---|---|
| `/login`, `/register`, `/verify-email`, `/forgot-password`, `/reset-password` | A |
| `/teachers`, `/teachers/:id`, `/subjects`, `/ranking` | A |
| `/teacher/profile`, `/teacher/subjects`, `/teacher/packages` | A |
| `/teacher/learning`, `/student/learning`, `/chat`, `/notifications` | A |
| `/checkout`, `/payment/*`, `/student/packages` | B |
| `/teacher/bookings`, `/student/bookings`, `/trials` | B |
| `/teacher/wallet`, `/teacher/payouts`, `/student/refunds` | B |
| `/admin/*` | B |

B đăng ký route trong router; A cung cấp lazy page exports từ feature `index.ts`.

## 4. Definition of Done

Một Frontend feature chỉ hoàn thành khi:

- API hooks và types khớp `API_CONTRACT.md`.
- Có loading, empty, error và success state.
- Form có client validation nhưng vẫn hiển thị được validation error từ Backend.
- Route guard và role/approval UX đúng đặc tả.
- Không deep import hoặc truy cập internals của feature khác.
- Có component/hook tests cho hành vi quan trọng.
- Responsive và keyboard navigation hoạt động.
- `npm run lint` và `npm run build` thành công.

## 5. Thứ tự tích hợp

1. App shell, API client và design system.
2. Auth/onboarding.
3. Marketplace.
4. Checkout/StudentPackage.
5. Booking.
6. Learning/Chat/Notification.
7. Finance/Ranking/Admin.
8. E2E và hardening.

Tích hợp FE với Backend vào cuối mỗi tuần, không chờ hoàn thành toàn bộ API mới bắt đầu nối dữ liệu thật.
