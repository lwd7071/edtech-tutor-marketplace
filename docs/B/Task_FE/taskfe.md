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

## B3: Student Dashboard Shell
- [ ] Student Dashboard Layout & Navigation
- [ ] Package summary card & Integration với Marketplace CTA

---

## B4: Checkout & Student Package
- [ ] PayOS Checkout flow & Redirect
- [ ] Màn hình Payment Success & Cancel kèm Invoice Polling
- [ ] Danh sách và chi tiết Student Package (bộ 4 chỉ số `SessionCounter`)

---

## B5: Booking & Session Report
- [ ] Calendar view (Lịch học tương tác Student / Teacher)
- [ ] Đặt lịch, hủy lịch và hoàn thành Booking
- [ ] Biểu mẫu Session Report & xem chi tiết
- [ ] Quản lý Trial Request

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
