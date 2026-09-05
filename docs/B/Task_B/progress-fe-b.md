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

## B3: Gói học sinh & Thanh toán payOS (Student Packages & Payments - ĐÃ HOÀN THÀNH 100%)

### 1. B3.1: Student Packages & Payments Types, API Clients & Query Hooks
- DTOs chuẩn xác theo API Contract: `StudentPackageSummary`, `StudentPackageDetail`, `StudentPackageStatus`, `InvoiceDetail`, `CreateInvoiceRequest`, `InvoiceStatus`.
- API Client `studentPackageApi.ts` & `paymentApi.ts`:
  - `GET /api/student/packages` (lấy danh sách gói học, phân trang & lọc theo status)
  - `GET /api/student/packages/{id}` (lấy chi tiết gói học)
  - `POST /api/student/invoices` (tạo hóa đơn thanh toán payOS)
  - `GET /api/student/invoices/{id}` (lấy chi tiết hóa đơn & trạng thái thanh toán)
- Hooks `useStudentPackages.ts` & `usePayments.ts`:
  - `useStudentPackages`, `useStudentPackageDetail`
  - `useCreateInvoice`
  - `useInvoiceDetail`: hỗ trợ polling 3s/lần (theo `SPEC-FE:E.5`), tự động dừng khi invoice đạt trạng thái terminal (`PAID`, `EXPIRED`, `CANCELLED`) hoặc component unmount.
- **TDD:** `useStudentPackages.test.tsx` (2/2 tests pass), `usePayments.test.tsx` (2/2 tests pass).

### 2. B3.2: Bộ chỉ số Buổi học SessionCounter & Thẻ Gói học StudentPackageCard
- `SessionCounter.tsx`: Hiển thị bộ 4 chỉ số buổi học (`Còn lại / Đang giữ / Đã học / Đã hoàn`) với màu sắc và tooltip trực quan.
- `StudentPackageCard.tsx`: Thẻ hiển thị gói học với tên gói, môn học, giáo viên, giá VND (`1.000.000 ₫` tabular-nums), tag trạng thái theo đúng bảng màu `SPEC-FE:6.5`, hạn dùng và nút điều hướng xem chi tiết.
- **TDD:** `SessionCounter.test.tsx` (pass), `StudentPackageCard.test.tsx` (pass).

### 3. B3.3: Giao diện Danh sách & Chi tiết Gói học
- `StudentPackageList.tsx`: Tabs chuyển trạng thái (`Tất cả`, `ACTIVE`, `COMPLETED`, `LOCKED_EXPIRED`), Grid thẻ gói học responsive (3 cột desktop, 2 cột tablet, 1 cột mobile), phân trang và Empty state.
- `StudentPackageDetailView.tsx`: Trang chi tiết gói học đầy đủ thông tin, thanh tiến độ học tập `Progress`, xử lý cảnh báo nghiệp vụ:
  - `LOCKED_EXPIRED`: Banner cảnh báo hết hạn kèm nút "Yêu cầu gia hạn" và "Yêu cầu hoàn tiền" (theo `SPEC-FE:3.4.3`).
  - `REFUND_PENDING`: Banner cảnh báo gói đang trong tiến trình xử lý hoàn tiền (theo `SPEC-FE:3.4.4`).
- Containers: `StudentPackagesPage.tsx`, `StudentPackageDetailPage.tsx`.
- **TDD:** `StudentPackageDetailView.test.tsx` (3/3 tests pass).

### 4. B3.4: Luồng Thanh toán payOS / VietQR & Polling Hóa đơn
- `CheckoutQRView.tsx`: Màn hình `/student/checkout/[invoiceId]` hiển thị mã QR VietQR từ payOS, liên kết thanh toán trực tiếp qua cổng payOS, đếm ngược hạn thanh toán, polling hóa đơn tự động 3 giây/lần và chuyển trang khi thanh toán thành công.
- `PaymentResultView.tsx`: Màn hình `/student/payment-result/[invoiceId]` xử lý giao diện kết quả theo trạng thái hóa đơn: `PAID` (thành công, nút dẫn tới gói học), `EXPIRED` (hết hạn, nút thử lại), `CANCELLED`.
- Containers: `CheckoutPage.tsx`, `PaymentResultPage.tsx`.
- **TDD:** `CheckoutQRView.test.tsx` (pass), `PaymentResultView.test.tsx` (pass).

### 5. B3.5: Student App Shell Layout & Route Protection (RoleGuard STUDENT)
- `StudentAppLayout.tsx`: Sidebar cố định 248px (`SPEC-FE:4.1 & 9.3`), top header 64px, breadcrumbs, menu icon trực quan (Tổng quan, Gói học của tôi, Lịch học, Bài tập, Tin nhắn, Hồ sơ), responsive drawer trên thiết bị di động.
- App Router Pages:
  - `src/app/student/layout.tsx` bọc trong `RoleGuard allowedRoles={['STUDENT']}`
  - `src/app/student/packages/page.tsx`
  - `src/app/student/packages/[id]/page.tsx`
  - `src/app/student/checkout/[invoiceId]/page.tsx`
  - `src/app/student/payment-result/[invoiceId]/page.tsx`
- Re-export sạch qua `src/features/student-packages/index.ts` và `src/features/payments/index.ts`.

---

## B5: Quản lý Lịch học & Báo cáo Buổi học (Bookings & Session Report - ĐÃ HOÀN THÀNH 100%)

### 1. B5.1: Booking Types, API Clients & Query Hooks
- DTOs chuẩn xác theo API Contract & SPEC-FE: `BookingDetail`, `BookingStatus`, `DeliveryMode`, `SessionReport`, `CreateBookingRequest`, `CompleteBookingRequest`, `CancelBookingRequest`, `TrialRequestView`.
- API Client `bookingApi.ts` với 8 endpoints:
  - `GET /api/student/bookings` (danh sách lịch học học sinh)
  - `POST /api/student/bookings` (tạo booking mới)
  - `POST /api/student/bookings/{id}/cancel` (hủy lịch học)
  - `POST /api/teacher/bookings/{id}/complete` (hoàn thành buổi học kèm SessionReport)
  - `GET /api/teacher/trial-requests` (danh sách yêu cầu học thử)
  - `POST /api/teacher/trial-requests/{id}/accept` (chấp nhận học thử)
  - `POST /api/teacher/trial-requests/{id}/reject` (từ chối học thử)
  - `POST /api/student/trial-requests` (học sinh gửi yêu cầu học thử)
- TanStack Query hooks: `useStudentBookings`, `useCreateBooking`, `useCompleteBooking`, `useCancelBooking`, `useTeacherTrialRequests`, `useAcceptTrialRequest`, `useRejectTrialRequest`, `useCreateTrialRequest`. Tự động invalidate queries liên quan khi mutate.
- **TDD:** `useBookings.test.tsx` pass 4/4 tests.

### 2. B5.2: Lịch học Tương tác & Chi tiết Buổi học
- `BookingStatusTag.tsx`: Tag trạng thái trực quan chuẩn màu `SPEC-FE:6.5` (`SCHEDULED`, `COMPLETED`, `CANCELLED`, `EXPIRED`).
- `BookingCard.tsx`: Hiển thị buổi học với định dạng thời gian Việt Nam `HH:mm – HH:mm · Thứ X, dd/MM/yyyy` (theo `SPEC-FE:1.4`), thông tin môn học, hình thức học (`ONLINE` / `OFFLINE`), link phòng học trực tuyến, nút xem chi tiết và hủy lịch.
- `BookingDetailDrawer.tsx`: Drawer xem chi tiết buổi học và thông tin `SessionReport` (nội dung bài dạy, nhận xét, đánh giá sao, link record, bài tập).
- `BookingCalendarView.tsx`: Giao diện danh sách lịch học với Tabs phân loại trạng thái (`Tất cả`, `Sắp tới`, `Đã học`, `Đã hủy`), responsive layout, nút "Đặt lịch học mới" và Empty state.
- **TDD:** `BookingCalendarView.test.tsx` pass.

### 3. B5.3: Hoàn thành Buổi học & Nộp SessionReport
- `SessionReportModal.tsx`: Form hoàn thành buổi học dành cho giáo viên nộp SessionReport theo `SPEC-FE:5.3.3`:
  - `lessonTopic` (bắt buộc): Chủ đề / nội dung giảng dạy
  - `studentFeedback` (bắt buộc): Đánh giá, nhận xét về học sinh
  - `studentRating`: Đánh giá 1–5 sao
  - `recordingUrl`: Đường dẫn xem lại video buổi học (tùy chọn)
  - `homeworkAssigned`: Nội dung bài tập về nhà giao cho học viên
- **TDD:** `SessionReportModal.test.tsx` pass.

### 4. B5.4: Hủy lịch & Đặt lịch Mới
- `CancelBookingModal.tsx`: Form hủy lịch học theo `SPEC-FE:5.3.2`, bắt buộc chọn lý do hủy theo người khởi tạo (`STUDENT_REQUEST` / `TEACHER_EMERGENCY`) kèm cảnh báo về chính sách hoàn giờ học.
- `CreateBookingModal.tsx`: Form đặt lịch học mới, chọn gói học, ngày giờ học, hình thức học (`ONLINE` / `OFFLINE`), phòng học/địa chỉ, tự động bắt lỗi xung đột lịch 409 `BOOKING_TIME_CONFLICT`.
- **TDD:** `CancelBookingModal.test.tsx` pass, `CreateBookingModal.test.tsx` pass.

### 5. B5.5: Student Booking Page & App Router
- `StudentBookingsPage.tsx`: Container trang quản lý lịch học của học sinh.
- App Router Page: `src/app/student/bookings/page.tsx` bọc trong `StudentAppLayout` với tiêu đề và breadcrumb rõ ràng.
- Export public interfaces qua `src/features/bookings/index.ts`.

---

## B6: Tích hợp Booking với Learning & Chat (ĐÃ HOÀN THÀNH 100%)

### 1. B6.1: Route Context & Inter-Module Link Helpers (`src/features/bookings/utils/routes.ts`)
- Cung cấp các tiện ích điều hướng chuẩn giữa các feature:
  - `getStudentBookingsRoute`: Trả về `/student/bookings`.
  - `getBookingDetailRoute`: Trả về link mở chi tiết buổi học.
  - `getChatRoute`: Tạo đường dẫn mở hội thoại 1-1 với gia sư/học sinh (`/student/messages?userId=...` hoặc `/teacher/messages?userId=...`).
  - `getAssignmentsRoute`: Tạo đường dẫn liên kết sang phân hệ Bài tập (`/student/assignments?bookingId=...`).
  - `getMeetingLink`: Chuẩn hóa và làm sạch liên kết phòng học trực tuyến (Google Meet, Zoom, MS Teams), loại bỏ URL độc hại (`javascript:`).
- **TDD:** `routes.test.ts` pass 4/4 tests.

### 2. B6.2: Hook `useUpcomingBooking` & Hàm tính toán độc lập (`useUpcomingBooking.ts`)
- Lọc buổi học sớm nhất sắp diễn ra ở trạng thái `SCHEDULED`.
- Tự động tính toán các cờ thời gian thực: `isHappeningNow`, `minutesUntilStart`, và `canJoinMeeting` (mở quyền vào lớp trước 15 phút).
- Hỗ trợ cả hai chế độ: Tự động query từ server hoặc tính toán từ danh sách bookings truyền vào.
- **TDD:** `useUpcomingBooking.test.tsx` pass 5/5 tests.

### 3. B6.3: Widget Thẻ Buổi học Sắp tới `UpcomingSessionCard`
- Thiết kế dạng thẻ Card/Banner nổi bật cho Dashboard (của Member A):
  - Badge trạng thái thời gian thực: "● Đang diễn ra" hoặc "Sắp bắt đầu (X phút nữa)".
  - Nút "Vào phòng học" (tự động enable khi đến giờ học), nút "Nhắn tin", nút "Xem chi tiết".
  - Hỗ trợ skeleton loading mượt mà.
- **TDD:** `UpcomingSessionCard.test.tsx` pass 4/4 tests.

### 4. B6.4: Tích hợp Nút Chat & Bài tập vào BookingCard & BookingDetailDrawer
- `BookingCard.tsx`: Bổ sung nút "Nhắn tin" với gia sư và nút "Bài tập" khi buổi học có dặn dò/giao bài.
- `BookingDetailDrawer.tsx`: Footer drawer tích hợp sẵn nút "Nhắn tin với gia sư" và "Xem bài tập" điều hướng trực tiếp sang Learning module.

### 5. B6.5: Re-export Public Interface
- Export toàn bộ public API qua `src/features/bookings/index.ts` để các feature của Member A import an toàn.

---

## B7: Quản trị Tài chính & Admin Dashboard (Finance & Admin - ĐÃ HOÀN THÀNH 100%)

### 1. B7.1: DTOs, API Clients & TanStack Query Hooks
- Định nghĩa đầy đủ DTOs theo SPEC-FE và API Contract của Backend:
  - `WalletView` (3 buckets: `availableBalanceVnd`, `pendingSettlementBalanceVnd`, `withdrawingBalanceVnd`), `LedgerEntryView` (hạch toán kép).
  - `BankAccountView`, `UpsertBankAccountRequest`.
  - `PayoutRequestView`, `CreatePayoutRequest`.
  - `RefundRequestView`, `CreateRefundRequest`.
  - `ExtensionRequestView`, `CreateExtensionRequest`.
  - `AdminDashboardView`, `PlatformSettingsView`, `AuditLogView`.
- API Clients:
  - `financeApi.ts`: 7 endpoints cho Teacher & Student (`/api/teacher/wallet`, `/api/teacher/ledger`, `/api/teacher/bank-accounts`, `/api/teacher/payouts`, `/api/student/refunds`, `/api/student/extensions`).
  - `adminFinanceApi.ts`: 8 endpoints cho Admin (`/api/admin/payouts`, `/api/admin/refunds`, `/api/admin/extensions`, `/api/admin/dashboard`, `/api/admin/settings`, `/api/admin/audit-logs`).
- TanStack Query hooks:
  - `useFinance.ts`: `useTeacherWallet`, `useTeacherLedger`, `useBankAccounts`, `usePayoutRequests`, `useStudentRequests`, cùng các mutations tự động invalidate queries.
  - `useAdminFinance.ts`: `useAdminFinanceQueues`, `useApprovePayout`, `useRejectPayout`, `useProcessRefund`, `useReviewExtension`, `useAdminDashboard`, `usePlatformSettings`, `useUpdatePlatformSettings`, `useAdminAuditLogs`.
- **TDD:** `financeApi.test.ts` (6 tests pass), `useFinance.test.tsx` (5 tests pass), `useAdminFinance.test.tsx` (4 tests pass).

### 2. B7.2: Ví Giáo viên & Sổ cái Hạch toán Kép
- `WalletSummaryCard.tsx`: Hiển thị 3 bucket số dư (Khả dụng, Chờ quyết toán, Đang rút), nút "Rút tiền" và "Tài khoản ngân hàng", format ` ₫`, `tabular-nums`.
- `LedgerTable.tsx`: Sổ cái tài chính hiển thị giao dịch với định dạng dấu `+` màu xanh lá (`#16a34a`) cho ghi có / tiền vào và `−` màu đỏ (`#dc2626`) cho ghi nợ / tiền ra. Hiển thị mã bút toán (monospace) và thời gian chuẩn `dd/MM/yyyy HH:mm`.
- **TDD:** `WalletSummaryCard.test.tsx` (3 tests pass), `LedgerTable.test.tsx` (2 tests pass).

### 3. B7.3: Rút tiền & Quản lý Tài khoản Ngân hàng
- `BankAccountList.tsx` & `BankAccountModal.tsx`: Danh sách tài khoản ngân hàng, gắn nhãn mặc định, modal thêm/sửa tài khoản chọn BIN ngân hàng VietQR.
- `CreatePayoutModal.tsx` & `PayoutListTable.tsx`: Form tạo yêu cầu rút tiền kèm validation số dư khả dụng, bảng theo dõi trạng thái yêu cầu rút (`PENDING`, `APPROVED`, `REJECTED`, `TRANSFERRED`).
- **TDD:** `PayoutComponents.test.tsx` (2 tests pass).

### 4. B7.4: Hoàn tiền & Gia hạn Gói học của Học sinh
- `CreateRefundModal.tsx`: Form gửi yêu cầu hoàn tiền cho các buổi chưa học, tính tiền ước tính tự động.
- `CreateExtensionModal.tsx`: Form gửi yêu cầu gia hạn ngày hết hạn của gói học.
- `StudentRequestsTable.tsx`: Bảng quản lý tổng hợp các yêu cầu hoàn tiền và gia hạn của học sinh với tabs chuyển đổi trực quan.
- **TDD:** `StudentFinanceComponents.test.tsx` (2 tests pass).

### 5. B7.5: Hàng đợi Duyệt Tài chính & Cài đặt Sàn của Admin
- `AdminPayoutTable.tsx`: Bảng duyệt yêu cầu rút tiền (Phê duyệt kèm mã tham chiếu chuyển khoản hoặc Từ chối kèm lý do).
- `AdminRefundTable.tsx`: Bảng xử lý yêu cầu hoàn tiền (Hoàn tiền hoặc Từ chối).
- `AdminExtensionTable.tsx`: Bảng duyệt yêu cầu gia hạn gói học (Chấp thuận gia hạn ngày hoặc Từ chối).
- `AdminDashboardOverview.tsx`: Thống kê tổng quan sàn (Doanh thu, số dư ví, số giao dịch, số học sinh & gia sư hoạt động).
- `AdminSettingsForm.tsx`: Form cấu hình sàn (Tỷ lệ hoa hồng platform %, hạn mức rút tiền tối thiểu/tối đa, số ngày gia hạn tối đa).
- `AdminAuditLogTable.tsx`: Bảng nhật ký kiểm toán hệ thống ghi lại toàn bộ hành động quản trị viên.
- **TDD:** `AdminFinanceTables.test.tsx` (3 tests pass), `AdminFinanceComponents.test.tsx` (3 tests pass).

### 6. B7.6: App Router Pages & Role Protection
- Next.js App Router:
  - Teacher: `/teacher/wallet`, `/teacher/bank-accounts`, `/teacher/payouts` (Bảo vệ bởi `RoleGuard: TEACHER` + `TeacherApprovalGuard`).
  - Student: `/student/requests` (Bảo vệ bởi `RoleGuard: STUDENT`).
  - Admin: `/admin/dashboard`, `/admin/payouts`, `/admin/refunds`, `/admin/extensions`, `/admin/settings`, `/admin/audit-logs` (Bảo vệ bởi `RoleGuard: ADMIN`).
- Re-export toàn bộ public API qua `src/features/finance/index.ts` và `src/features/admin/index.ts`.

---

## B8: Hardening, Regression Testing & Hoàn tất Phân hệ B (ĐÃ HOÀN THÀNH 100%)

### 1. B8.1: Kiểm thử Hồi quy Toàn diện (Regression Testing)
- Toàn bộ các luồng B1 -> B7 được kiểm thử đồng thời:
  - B1: Core Network, Axios Refresh Mutex Queue, Error Mapping, AppProviders, Route Guards.
  - B2: Teacher Approval, Subject Proposal Approval, User Moderation.
  - B3: Student Packages, Session Counter, VietQR Checkout, Payment Polling.
  - B5: Bookings, Calendar View, Session Report, Cancellation, Booking Conflict 409 handling.
  - B6: Inter-module routing, useUpcomingBooking real-time helper, UpcomingSessionCard widget.
  - B7: Teacher Wallet, Ledger, Bank Accounts, Payouts, Refunds, Extensions, Admin Queues, Platform Settings, Audit Logs, Admin Dashboard.
- **Kết quả Unit Test:** **45/45 test suites PASS**, **107/107 unit tests PASS 100%**.

### 2. B8.2: Next.js Production Build
- **Kiểm tra TypeScript:** Hoàn toàn sạch, 0 lỗi type (`Finished TypeScript in 12.6s`).
- **Static Generation:** Biên dịch thành công 20/20 routes (17 Static prerendered, 3 Dynamic server-rendered).
- **Mã phản hồi HTTP & Guard:** Tất cả các route đều được bao bọc `RoleGuard` ngăn chặn truy cập trái phép.

---

## Tổng kết Bộ chỉ số Hoàn thành của Thành viên B
- **Số Sprint đã hoàn thành:** 6/6 (B1, B2, B3, B5, B6, B7, B8).
- **Tổng số Test Suites:** **45/45 PASS (100%)**.
- **Tổng số Unit Tests:** **107/107 PASS (100%)**.
- **Trạng thái Build:** **Compiled successfully (Turbopack, Next.js 16.3.4, 0 errors)**.
- **Tuân thủ Thiết kế:** 100% SPEC-FE (màu Teal `#0F766E`, font số `tabular-nums`, múi giờ `Asia/Ho_Chi_Minh`, định dạng tiền tệ VND ` ₫`, thời gian `dd/MM/yyyy HH:mm`).




