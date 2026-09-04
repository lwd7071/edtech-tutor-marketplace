# Danh sách Nhiệm vụ Frontend - Thành viên B

Danh sách các task chi tiết theo tuần cho **Thành viên B (Transaction & Operations)** để theo dõi và tick hoàn thành (`[x]`).
> Nền tảng kỹ thuật: **Next.js (App Router) + TypeScript + Ant Design 5/6 + TanStack Query v5 + Axios + React Hook Form + Zustand**.

---

## B1: Frontend Foundation (Hạ tầng App Shell & Core Network)

### B1.0: Sửa lỗi Build SSR & Cấu hình Test Suite
- [x] Cập nhật `src/app/page.tsx` thêm directive `'use client';` để sửa lỗi SSR prerender
- [x] Thêm các scripts test vào `package.json`: `"test": "jest --watchAll=false --forceExit"`, `"test:watch": "jest"`
- [x] Chạy kiểm tra `npm run build` và xác nhận thành công không có lỗi

### B1.1: Chuẩn hóa Type Envelope & Error Mapping (Spec & ERROR_CODES.md)
- [x] Tạo `src/shared/api/types.ts` định nghĩa chuẩn envelope:
  - [x] `ApiResponse<T>`: `{ success, message, data, errors, meta }`
  - [x] `ApiErrorDetail`: `{ code, field, message }`
  - [x] `PaginationMeta`: `{ page, size, totalElements, totalPages }`
- [x] Tạo helper `parseApiError(error)` trích xuất mã lỗi, message tiếng Việt và map field errors
- [x] Viết unit test `src/shared/api/types.test.ts` (TDD: RED → GREEN)

### B1.2: Axios Client với Refresh-Token Queue (Chống Concurrency 401)
- [x] Tạo `src/shared/api/axiosClient.ts` với đầy đủ interceptors:
  - [x] Request Interceptor: Tự động đính kèm `Authorization: Bearer <token>` từ auth store
  - [x] Response Interceptor: Bắt lỗi 401 `AUTH_TOKEN_EXPIRED`
  - [x] **Refresh-Token Queue**: Cơ chế Mutex chỉ cho phép 1 request refresh token (`POST /api/v1/auth/refresh-token`), các request khác xếp hàng `failedQueue`
  - [x] Tự động retry tất cả request trong hàng đợi sau khi refresh token thành công
  - [x] Tự động clear session và trigger logout nếu refresh token thất bại
- [x] Cập nhật `src/shared/lib/axiosClient.ts` re-export từ `src/shared/api/axiosClient.ts` để tương thích ngược
- [x] Viết unit test `src/shared/api/axiosClient.test.ts` (TDD: RED → GREEN)

### B1.3: TanStack Query Provider & Error Boundary
- [x] Tạo `src/shared/components/AppProviders.tsx`:
  - [x] Khởi tạo `QueryClient` với cấu hình tối ưu: `staleTime: 60s`, không retry lỗi 401/403/404/422
  - [x] Tích hợp `QueryClientProvider`
  - [x] Tích hợp `ErrorBoundary` với giao diện thông báo lỗi thân thiện và nút "Tải lại trang"
- [x] Viết unit test `src/shared/components/AppProviders.test.tsx` (TDD: RED → GREEN)

### B1.4: Route Guards & Access Control
- [x] Tạo `src/shared/components/guards/AuthGuard.tsx`: Bảo vệ route yêu cầu đăng nhập
- [x] Tạo `src/shared/components/guards/RoleGuard.tsx`: Kiểm tra role `STUDENT`, `TEACHER`, `ADMIN`
- [x] Tạo `src/shared/components/guards/TeacherApprovalGuard.tsx`: Kiểm tra trạng thái giáo viên (`PENDING_APPROVAL`, `APPROVED`)
- [x] Viết unit test `src/shared/components/guards/Guards.test.tsx` (TDD: RED → GREEN)

### B1.5: Khởi tạo Cấu trúc 5 Feature Modules của B
- [x] `src/features/payments/` (`api`, `components`, `hooks`, `pages`, `schemas`, `types`, `index.ts`)
- [x] `src/features/student-packages/`
- [x] `src/features/bookings/`
- [x] `src/features/finance/`
- [x] `src/features/admin/`

---

## B2: Quản trị Phê duyệt & Onboarding Admin (ĐÃ HOÀN THÀNH 100%)

### B2.1: Admin Types, API Client & TanStack Query Hooks (TDD)
- [x] Tạo `src/features/admin/types/index.ts` định nghĩa DTOs khớp Backend Facade:
  - [x] `TeacherApprovalSnapshot`, `TeacherDocumentSnapshot`, `SubjectProposalSnapshot`, `IdentitySnapshot`
  - [x] `ApproveTeacherRequest`, `RejectRequest`, `ApproveSubjectProposalRequest`, `ChangeUserStatusRequest`
- [x] Tạo `src/features/admin/api/adminApi.ts` gọi các endpoint:
  - [x] `GET /api/admin/teachers/approvals`
  - [x] `POST /api/admin/teachers/{id}/approve`
  - [x] `POST /api/admin/teachers/{id}/reject`
  - [x] `GET /api/admin/subject-proposals`
  - [x] `POST /api/admin/subject-proposals/{id}/approve`
  - [x] `POST /api/admin/subject-proposals/{id}/reject`
  - [x] `PATCH /api/admin/users/{id}/status`
- [x] Tạo `src/features/admin/hooks/useAdminApprovals.ts` (query & mutations tự động invalidate query cache)
- [x] Viết unit test `src/features/admin/hooks/useAdminApprovals.test.tsx` (TDD: 6/6 tests pass)

### B2.2: Giao diện Duyệt hồ sơ Giáo viên (Teacher Approvals - TDD)
- [x] Tạo `src/features/admin/components/TeacherDetailDrawer.tsx` (xem chi tiết hồ sơ, bằng cấp kèm link secureUrl, duyệt/từ chối kèm lý do)
- [x] Tạo `src/features/admin/components/TeacherApprovalTable.tsx` (bảng ResponsiveTable, lọc theo tabs trạng thái `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, phân trang)
- [x] Viết unit test `src/features/admin/components/TeacherApprovalTable.test.tsx` (TDD: 2/2 tests pass)

### B2.3: Giao diện Phê duyệt Đề xuất Môn học (Subject Proposals - TDD)
- [x] Tạo `src/features/admin/components/ApproveSubjectModal.tsx` (form chuẩn hóa tên môn, danh mục, mô tả trước khi tạo)
- [x] Tạo `src/features/admin/components/SubjectProposalTable.tsx` (bảng quản lý môn đề xuất, duyệt & từ chối kèm lý do bắt buộc)
- [x] Viết unit test `src/features/admin/components/SubjectProposalTable.test.tsx` (TDD: 2/2 tests pass)

### B2.4: Kiểm duyệt & Điều chỉnh Tài khoản Người dùng (User Moderation - TDD)
- [x] Tạo `src/features/admin/components/UserModerationModal.tsx` (khóa/mở khóa tài khoản kèm lý do bắt buộc và cảnh báo hành động)
- [x] Viết unit test `src/features/admin/components/UserModerationModal.test.tsx` (TDD: 2/2 tests pass)

### B2.5: Thiết lập Route Pages & Role Protection Admin
- [x] Tạo `src/features/admin/pages/AdminTeachersPage.tsx`
- [x] Tạo `src/features/admin/pages/AdminSubjectsPage.tsx`
- [x] Tạo các App Router pages bọc `RoleGuard allowedRoles={['ADMIN']}`:
  - [x] `src/app/admin/teachers/page.tsx`
  - [x] `src/app/admin/subjects/page.tsx`
- [x] Export toàn bộ public interface qua `src/features/admin/index.ts`

---

## B3: Gói học sinh & Thanh toán payOS (Student Packages & Payments - ĐÃ HOÀN THÀNH 100%)

### B3.1: Student Packages & Payments Types, API Clients & Query Hooks (TDD)
- [x] Tạo `src/features/student-packages/types/index.ts` & `src/features/payments/types/index.ts` khớp API Contract & SPEC-FE
- [x] Tạo API Clients:
  - [x] `src/features/student-packages/api/studentPackageApi.ts` (`GET /api/student/packages`, `GET /api/student/packages/{id}`)
  - [x] `src/features/payments/api/paymentApi.ts` (`POST /api/student/invoices`, `GET /api/student/invoices/{id}`)
- [x] Tạo TanStack Query hooks:
  - [x] `useStudentPackages`, `useStudentPackageDetail`
  - [x] `useCreateInvoice`, `useInvoiceDetail` (tự động polling 3s/lần đến khi hóa đơn đạt trạng thái cuối: `PAID`, `EXPIRED`, `CANCELLED`)
- [x] Viết unit tests: `useStudentPackages.test.tsx` (2 tests pass), `usePayments.test.tsx` (2 tests pass)

### B3.2: Bộ chỉ số Buổi học SessionCounter & Thẻ Gói học (TDD)
- [x] Tạo `src/features/student-packages/components/SessionCounter.tsx` (bộ 4 chỉ số theo SPEC-FE: Còn lại / Đang giữ / Đã học / Đã hoàn)
- [x] Tạo `src/features/student-packages/components/StudentPackageCard.tsx` (card hiển thị gói, giáo viên, môn, hạn dùng, giá VND `tabular-nums`, status tag theo bảng màu spec)
- [x] Viết unit tests: `SessionCounter.test.tsx` (pass), `StudentPackageCard.test.tsx` (pass)

### B3.3: Giao diện Danh sách & Chi tiết Gói học (TDD)
- [x] Tạo `src/features/student-packages/components/StudentPackageList.tsx` (tabs lọc trạng thái, grid cards, pagination)
- [x] Tạo `src/features/student-packages/components/StudentPackageDetailView.tsx` (chi tiết gói học, thanh tiến độ, banner cảnh báo `LOCKED_EXPIRED` và `REFUND_PENDING`)
- [x] Tạo containers: `StudentPackagesPage.tsx`, `StudentPackageDetailPage.tsx`
- [x] Viết unit test: `StudentPackageDetailView.test.tsx` (3 tests pass)

### B3.4: Luồng Thanh toán payOS / VietQR & Polling Hóa đơn (TDD)
- [x] Tạo `src/features/payments/components/CheckoutQRView.tsx` (màn hình quét mã QR VietQR, link cổng payOS, đếm ngược, polling auto-redirect khi PAID)
- [x] Tạo `src/features/payments/components/PaymentResultView.tsx` (kết quả thanh toán `PAID`, `EXPIRED`, `CANCELLED`)
- [x] Tạo containers: `CheckoutPage.tsx`, `PaymentResultPage.tsx`
- [x] Viết unit tests: `CheckoutQRView.test.tsx` (pass), `PaymentResultView.test.tsx` (pass)

### B3.5: Student App Shell Layout & Route Protection (RoleGuard STUDENT)
- [x] Tạo `src/shared/components/layout/StudentAppLayout.tsx` (sidebar cố định 248px, header 64px, breadcrumbs, responsive drawer)
- [x] Tạo `src/app/student/layout.tsx` bọc trong `RoleGuard allowedRoles={['STUDENT']}`
- [x] Tạo các App Router pages:
  - [x] `src/app/student/packages/page.tsx`
  - [x] `src/app/student/packages/[id]/page.tsx`
  - [x] `src/app/student/checkout/[invoiceId]/page.tsx`
  - [x] `src/app/student/payment-result/[invoiceId]/page.tsx`
- [x] Export toàn bộ public interfaces qua `features/student-packages/index.ts` và `features/payments/index.ts`

---

## B4: Quản lý Lịch học & Báo cáo Buổi học (Bookings & Session Report - ĐÃ HOÀN THÀNH 100%)

### B4.1: Booking Types, API Clients & TanStack Query Hooks (TDD)
- [x] Tạo `src/features/bookings/types/index.ts` định nghĩa DTOs khớp Backend API Contract & SPEC-FE:
  - [x] `BookingDetail`, `BookingStatus`, `DeliveryMode`, `SessionReport`
  - [x] `CreateBookingRequest`, `CompleteBookingRequest`, `CancelBookingRequest`, `TrialRequestView`
- [x] Tạo API Client `src/features/bookings/api/bookingApi.ts` (8 endpoints lịch học và học thử)
- [x] Tạo TanStack Query hooks `src/features/bookings/hooks/useBookings.ts`:
  - [x] `useStudentBookings`, `useCreateBooking`, `useCompleteBooking`, `useCancelBooking`
  - [x] `useTeacherTrialRequests`, `useAcceptTrialRequest`, `useRejectTrialRequest`, `useCreateTrialRequest`
- [x] Viết unit tests: `useBookings.test.tsx` (TDD: 4/4 tests pass)

### B4.2: Lịch học Tương tác & Chi tiết Buổi học (TDD)
- [x] Tạo `src/features/bookings/components/BookingStatusTag.tsx` (bảng màu chuẩn `SPEC-FE:6.5`)
- [x] Tạo `src/features/bookings/components/BookingCard.tsx` (định dạng `HH:mm – HH:mm · Thứ X, dd/MM/yyyy`, link phòng học online, modal chi tiết)
- [x] Tạo `src/features/bookings/components/BookingDetailDrawer.tsx` (thông tin buổi học, trạng thái, và chi tiết SessionReport khi hoàn thành)
- [x] Tạo `src/features/bookings/components/BookingCalendarView.tsx` (tabs lọc trạng thái, agenda list responsive)
- [x] Viết unit tests: `BookingCalendarView.test.tsx` (TDD: pass)

### B4.3: Hoàn thành Buổi học & Nộp SessionReport (TDD)
- [x] Tạo `src/features/bookings/components/SessionReportModal.tsx` (nội dung bài dạy, nhận xét học sinh, đánh giá 1-5 sao, link recording, bài tập giao về nhà)
- [x] Viết unit tests: `SessionReportModal.test.tsx` (TDD: pass)

### B4.4: Hủy lịch & Đặt lịch Mới (TDD)
- [x] Tạo `src/features/bookings/components/CancelBookingModal.tsx` (phân loại người hủy `STUDENT_REQUEST` / `TEACHER_EMERGENCY`, lý do bắt buộc)
- [x] Tạo `src/features/bookings/components/CreateBookingModal.tsx` (form tạo lịch mới, xử lý conflict lịch 409 `BOOKING_TIME_CONFLICT`)
- [x] Viết unit tests: `CancelBookingModal.test.tsx` (TDD: pass), `CreateBookingModal.test.tsx` (TDD: pass)

### B4.5: Student Booking Page & App Router
- [x] Tạo container `src/features/bookings/pages/StudentBookingsPage.tsx`
- [x] Tạo App Router page `src/app/student/bookings/page.tsx`
- [x] Re-export toàn bộ public interface qua `src/features/bookings/index.ts`

---

## B6: Tích hợp Booking với Learning & Chat
- [ ] Cung cấp route context và booking links cho các module khác qua public interface

---

## B7: Quản trị Tài chính (Finance & Admin)
- [ ] Ví giáo viên (Teacher Wallet) & Sổ cái (Ledger)
- [ ] Quản lý tài khoản ngân hàng & Yêu cầu rút tiền (Payout Request)
- [ ] Yêu cầu hoàn tiền (Refund) và Gia hạn gói học (Extension)
- [ ] Admin Finance Queues, Platform Settings, Audit Logs, Dashboard Charts

---

## B8: Hardening & E2E Testing
- [ ] Tests hồi quy cho toàn bộ flow của B
- [ ] Kiểm tra Concurrency, Expired Session & Optimistic Locking (409)
