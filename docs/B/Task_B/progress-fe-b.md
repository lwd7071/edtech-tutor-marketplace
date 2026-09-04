# Báo cáo Tiến độ Frontend - Thành viên B

## Tổng quan
Thành viên B chịu trách nhiệm về **Transaction & Operations** và **Hạ tầng App Shell & Core Network**.
Toàn bộ quy trình phát triển được triển khai theo chuẩn mực **Test-Driven Development (TDD)**:
`Viết Test (RED) → Viết mã tối thiểu (GREEN) → Tối ưu hóa (REFACTOR)`.

---

## B1: Hạ tầng App Shell, Core Network & Route Protection (ĐÃ HOÀN THÀNH 100%)

### 1. B1.0: Sửa lỗi Build SSR & Cấu hình Test Suite
- Đã khắc phục lỗi `Element type is invalid: expected a string... but got: undefined` trên trang chủ `src/app/page.tsx` bằng directive `'use client';`.
- Bổ sung scripts test vào `frontend/package.json`: `"test": "jest --watchAll=false --forceExit"`, `"test:watch": "jest"`.
- Đã chạy kiểm thử build production của Next.js (Turbopack): compile thành công, TypeScript check sạch 100%, render 5/5 static pages không lỗi.

### 2. B1.1: Chuẩn hóa Type Envelope & Error Mapping (`src/shared/api/types.ts`)
- Định nghĩa chuẩn envelope theo `API_CONTRACT.md` và `ERROR_CODES.md`:
  - `ApiResponse<T>`: `{ success, message, data, errors, meta }`
  - `ApiErrorDetail`: `{ code, field, message }`
  - `PaginationMeta`: `{ page, size, totalElements, totalPages }`
- Xây dựng helper `parseApiError(error)` trích xuất mã lỗi, message tiếng Việt, và map `fieldErrors` tự động cho Form UI.
- **TDD:** `types.test.ts` pass 4/4 tests.

### 3. B1.2: Axios Client với Refresh-Token Queue (`src/shared/api/axiosClient.ts`)
- Tự động gắn Bearer Token vào header request nếu user đã đăng nhập.
- **Refresh-Token Queue (Mutex Pattern)**: Khi có nhiều request đồng thời gặp lỗi 401:
  - Chỉ 1 request đầu tiên trigger gọi `POST /api/v1/auth/refresh-token`.
  - Các request còn lại xếp hàng vào `failedQueue`.
  - Khi refresh thành công: tự động cập nhật token trong Zustand store và retry lại toàn bộ request trong queue.
  - Khi refresh thất bại: clear session, logout và reject toàn bộ queue.
- Re-export tại `src/shared/lib/axiosClient.ts` đảm bảo tính tương thích ngược với code của Member A.
- **TDD:** `axiosClient.test.ts` pass 4/4 tests.

### 4. B1.3: TanStack Query Provider & Global Error Boundary (`src/shared/components/AppProviders.tsx`)
- Khởi tạo `QueryClient` với cấu hình tối ưu: `staleTime: 60s`, `gcTime: 5m`, không retry với các mã lỗi client 401, 403, 404, 422.
- Tích hợp `ErrorBoundary` bắt các ngoại lệ runtime của React Component, hiển thị giao diện báo lỗi thân thiện kèm nút "Thử lại".
- Tích hợp `AppProviders` vào `src/app/layout.tsx` bọc lấy toàn bộ ứng dụng.
- **TDD:** `AppProviders.test.tsx` pass 2/2 tests.

### 5. B1.4: Route Guards & Access Control (`src/shared/components/guards/`)
- `AuthGuard`: Kiểm tra trạng thái đăng nhập, tự động chuyển hướng về `/auth/login?redirect=...`.
- `RoleGuard`: Kiểm tra vai trò `STUDENT`, `TEACHER`, `ADMIN`, hiển thị thông báo 403 thân thiện nếu không đủ quyền.
- `TeacherApprovalGuard`: Kiểm tra trạng thái giáo viên, hiển thị cảnh báo tài khoản đang chờ phê duyệt nếu `status === 'PENDING'`.
- **TDD:** `Guards.test.tsx` pass 6/6 tests.

### 6. B1.5: Khởi tạo 5 Feature Modules của B
Đã khởi tạo cấu trúc thư mục chuẩn Feature-Sliced cho cả 5 modules:
- `src/features/payments/` (`types`, `index.ts`)
- `src/features/student-packages/` (`types`, `index.ts`)
- `src/features/bookings/` (`types`, `index.ts`)
- `src/features/finance/` (`types`, `index.ts`)
- `src/features/admin/` (`types`, `index.ts`)

---

## B2: Quản trị Phê duyệt & Onboarding Admin (ĐÃ HOÀN THÀNH 100%)

### 1. B2.1: Admin Types, API Client & TanStack Query Hooks (`src/features/admin/`)
- Khởi tạo đầy đủ DTOs theo spec Backend Facade: `TeacherApprovalSnapshot`, `TeacherDocumentSnapshot`, `SubjectProposalSnapshot`, `IdentitySnapshot`, `ApproveTeacherRequest`, `RejectRequest`, `ApproveSubjectProposalRequest`, `ChangeUserStatusRequest`.
- API Client `adminApi.ts` tích hợp đầy đủ 7 endpoints quản trị:
  - `GET /api/admin/teachers/approvals`
  - `POST /api/admin/teachers/{id}/approve`
  - `POST /api/admin/teachers/{id}/reject`
  - `GET /api/admin/subject-proposals`
  - `POST /api/admin/subject-proposals/{id}/approve`
  - `POST /api/admin/subject-proposals/{id}/reject`
  - `PATCH /api/admin/users/{id}/status`
- TanStack Query hooks `useAdminApprovals.ts`: tự động quản lý cache và invalidate query key tương ứng sau khi mutate thành công.
- **TDD:** `useAdminApprovals.test.tsx` pass 6/6 tests.

### 2. B2.2: Giao diện Duyệt hồ sơ Giáo viên (`TeacherApprovalTable.tsx` & `TeacherDetailDrawer.tsx`)
- Drawer chi tiết giáo viên hiển thị đầy đủ thông tin: Trình độ học vấn, số năm kinh nghiệm, tiểu sử, danh sách bằng cấp/chứng chỉ kèm link xem trực tiếp qua `secureUrl`.
- Hỗ trợ form nhập lý do từ chối bắt buộc (tránh từ chối không có nguyên nhân cụ thể).
- Bảng quản lý `ResponsiveTable` hỗ trợ tabs chuyển trạng thái (`PENDING_APPROVAL`, `APPROVED`, `REJECTED`) và phân trang đồng bộ với server.
- **TDD:** `TeacherApprovalTable.test.tsx` pass 2/2 tests.

### 3. B2.3: Giao diện Phê duyệt Đề xuất Môn học (`SubjectProposalTable.tsx` & `ApproveSubjectModal.tsx`)
- Modal `ApproveSubjectModal` cho phép Admin chuẩn hóa tên môn học, danh mục môn học, và bổ sung mô tả trước khi tạo môn học chính thức vào hệ thống.
- Bảng hiển thị danh sách các đề xuất từ giáo viên, tích hợp nút "Phê duyệt" và "Từ chối" kèm nhập lý do từ chối.
- **TDD:** `SubjectProposalTable.test.tsx` pass 2/2 tests.

### 4. B2.4: Kiểm duyệt & Điều chỉnh Tài khoản Người dùng (`UserModerationModal.tsx`)
- Modal thao tác khóa / mở khóa tài khoản người dùng (`ACTIVE` <-> `LOCKED`).
- Bắt buộc nhập lý do điều chỉnh trạng thái tài khoản và hiển thị cảnh báo rủi ro thao tác.
- **TDD:** `UserModerationModal.test.tsx` pass 2/2 tests.

### 5. B2.5: Thiết lập Route Pages & Role Protection Admin
- `AdminTeachersPage.tsx` và `AdminSubjectsPage.tsx` đóng gói giao diện trang quản trị.
- Next.js App Router pages:
  - `src/app/admin/teachers/page.tsx`
  - `src/app/admin/subjects/page.tsx`
- Cả hai page được bọc qua `<RoleGuard allowedRoles={['ADMIN']}>`, ngăn chặn truy cập trái phép và hiển thị màn hình 403 thân thiện nếu không có quyền.
- Export public API sạch sẽ qua `src/features/admin/index.ts`.

---

## Kết quả Kiểm thử & Chẩn đoán Toàn diện
- **Unit Tests:** **20/20 test suites pass, 43/43 unit tests pass 100%**.
- **Next.js Production Build:** **Compiled & static generation 7/7 pages thành công** (bao gồm `/admin/teachers`, `/admin/subjects`).
- **TypeScript:** Type check sạch 100%, không phát sinh bất kỳ lỗi compile nào.

