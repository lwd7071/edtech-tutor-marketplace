# Tiến độ Backend — Thành viên B

## 2026-09-04 — Task 8 (Tuần 8) Hardening & Invariants Testing, Demo Seed Data, Dockerization & Final Verification

### Đã thực hiện

- **Task 8.1 (Hardening Invariants Integration Test):**
  - Viết bộ test tích hợp `HardeningInvariantsIntegrationTest` kiểm chứng đầy đủ 7 invariants cốt lõi:
    1. `ledger_integrity_invariant`: $\sum(\text{LedgerEntry}) = \text{Wallet bucket balances}$ (`PENDING`, `AVAILABLE`, `RESERVED`).
    2. `package_counter_invariant`: $\text{remaining} + \text{reserved} + \text{completed} + \text{refunded} = \text{totalSessions}$.
    3. `gist_exclusion_double_booking_invariant`: GiST index `ex_booking_teacher_overlap` chặn double-booking cùng teacher trong cùng slot thời gian `[start_time, end_time)`.
    4. `booking_cancellation_and_slot_release_invariant`: Khi cancel booking, trạng thái là `CANCELLED`, `is_deleted = false`, giải phóng slot thời gian để book lại được.
    5. `cumulative_rounding_precision_invariant`: Thuật toán floor() và tính lũy kế không thất thoát 1 đồng lẻ nào.
    6. `soft_delete_and_version_invariant`: Entity soft delete set `is_deleted = true`, tăng optimistic lock version.
    7. `append_only_immutability_invariant`: Bảng tài chính / audit log là append-only, không có cột `is_deleted` hay `updated_at`.
- **Task 8.2 (Demo Seed Data Migration):**
  - Tạo `V23__seed_demo_data.sql`: Seed đầy đủ dữ liệu mẫu chuẩn schema: 1 Admin, 3 Teachers (`APPROVED`), 3 Subjects, 4 Pricing Packages, 3 Students (`ACTIVE`), 2 Invoices (`PAID`), 2 Student Packages (`ACTIVE`), 2 Bookings & Session Report, Wallets & Bank Accounts, 4 Ledger Entries.
  - Viết test `FlywayV23DemoDataTest` và cập nhật `FlywayMigrationTest` kiểm tra toàn bộ 23 migration chạy sạch sẽ từ DB rỗng.
- **Task 8.3 (Dockerization & Deployment):**
  - Tạo `backend/Dockerfile` multi-stage (Eclipse Temurin 21 JRE, layertools, non-root user `appuser`, tối ưu JVM G1GC).
  - Tạo `backend/.dockerignore`.
  - Cập nhật `docker-compose.yml` bổ sung service `backend` kết nối `postgres` và `redis`.
- **Task 8.4 (Final Verification):**
  - Chạy toàn bộ test suite `mvn test` với Testcontainers PostgreSQL 16 và Redis 7 thật: **240/240 tests PASS 100% (0 failures, 0 errors)**.

### Evidence

| Kiểm tra | Run | Failure | Error | Skipped | Kết quả |
|---|---:|---:|---:|---:|---|
| Hardening Invariants Integration (`HardeningInvariantsIntegrationTest`) | 7 | 0 | 0 | 0 | Pass |
| Flyway Migrations V1–V23 (`FlywayMigrationTest`, `FlywayV23DemoDataTest`, `FlywayV22BookingDomainTest`, `FlywayV21PaymentDomainTest`, `FlywayV18LegacyDataTest`) | 12 | 0 | 0 | 0 | Pass |
| Finance, Booking, Payment, Admin, Auth, Catalog, Ranking, Architecture Suites | 221 | 0 | 0 | 0 | Pass |
| **Full Maven Test Suite (kèm Testcontainers Docker thật)** | **240** | **0** | **0** | **0** | **`BUILD SUCCESS`** |

---

## 2026-09-04 — Task 7 (Tuần 7) Finance, Admin Dashboard, Platform Settings, Audit Logs & Scheduled Jobs

### Đã thực hiện

- **Task 7.1 (Teacher BankAccount CRUD):**
  - Tạo entity TeacherBankAccount (hỗ trợ partial unique index is_default, mã hóa số tài khoản bằng AES-GCM với AccountNumberCipher, trả về ccountNumberMasked dạng ******1234).
  - Triển khai BankAccountService và TeacherBankAccountController đầy đủ các endpoint GET, POST 201, PUT, DELETE 204 theo đúng API Contract.
- **Task 7.2 (Payout Request Flow):**
  - Tạo entity PayoutRequest (status: PENDING $\rightarrow$ PROCESSING $\rightarrow$ SUCCEEDED / REJECTED / FAILED), bổ sung Wallet.debitReserved.
  - Triển khai PayoutService, TeacherPayoutController và AdminPayoutController.
  - Lock thứ tự an toàn: Wallet (pessimistic) $\rightarrow$ chuyển vailable $\rightarrow$ eserved $\rightarrow$ ghi 2 LedgerEntry (PAYOUT_RESERVED), hoàn tất hoặc từ chối hoàn trả số dư khả dụng (PAYOUT_RELEASED).
- **Task 7.3 (Refund Request Flow):**
  - Mở rộng StudentPackage với các phương thức markRefundPending, estoreFromRefundPending, pplyRefund.
  - Mở rộng EnrollmentFacade và BookingEligibilityFacade (kiểm tra hasScheduledBookingForPackage).
  - Tạo entity RefundRequest và triển khai RefundService, StudentRefundController, AdminRefundController.
  - Khóa gói sang REFUND_PENDING ngay khi tạo; tính toán hoàn tiền theo công thức số nguyên tích lũy loor(...) tránh lệch số lẻ; khấu trừ ví PENDING của giáo viên qua LedgerEntry (REFUND_DEBIT_PENDING).
- **Task 7.4 (Package Extension Request Flow):**
  - Tạo entity PackageExtensionRequest và triển khai ExtensionService, StudentExtensionController, AdminExtensionController.
  - Chỉ cho phép tạo yêu cầu với gói LOCKED_EXPIRED; Admin phê duyệt bắt buộc pprovedExpiryDate > now và kích hoạt lại gói ACTIVE.
- **Task 7.5 (Wallet & Ledger API):**
  - Tạo WalletView, LedgerEntryView, WalletQueryService và TeacherWalletController (GET /api/teacher/wallet, GET /api/teacher/wallet/ledger phân trang).
- **Task 7.6 (Admin Dashboard, Settings & Audit Logs):**
  - Tạo JPA entity PlatformSettings, PlatformSettingsRepository, AdminSettingsService, AdminSettingsController (GET /api/admin/settings, PUT /api/admin/settings).
  - Refactor PlatformSettingsFacadeImpl dùng PlatformSettingsRepository thay cho JdbcTemplate.
  - Tạo AdminDashboardView, AdminDashboardRepository, AdminDashboardService, AdminDashboardController (GET /api/admin/dashboard thống kê GMV, commission, users, bookings, pending payouts/refunds).
  - Mở rộng AuditLogRepository, tạo AuditLogView, AuditLogQueryService và AdminAuditLogController (GET /api/admin/audit-logs phân trang, bộ lọc actor/action/target).
- **Task 7.7 (Scheduled Jobs Finalization & Idempotency):**
  - Rà soát 5 background jobs (BookingExpiryJob, BookingReminderJob, PackageExpiryJob, InvoiceExpiryJob, TeacherStatsJob): đều có ShedLock @SchedulerLock, idempotent và xử lý ngoại lệ an toàn.
- **Task 7.8 (Architecture & Full Test Verification):**
  - Tách hoàn toàn dependency của Admin Controllers khỏi inance.domain.*Status để đạt 100% tuân thủ ArchUnit.
  - Chạy full mvn clean test với Testcontainers PostgreSQL Docker thật: **229/229 tests PASS 100%**, không có test bị skip hay fail.

### Evidence

| Kiểm tra | Run | Failure | Error | Skipped | Kết quả |
|---|---:|---:|---:|---:|---|
| Finance Domain & Service Tests (BankAccountServiceTest, PayoutServiceTest, RefundServiceTest, ExtensionServiceTest, WalletTest, LedgerEntryTest) | 20 | 0 | 0 | 0 | Pass |
| Finance & Admin Controllers (TeacherBankAccountControllerTest, TeacherPayoutControllerTest, StudentRefundControllerTest, StudentExtensionControllerTest, TeacherWalletControllerTest, AdminPayoutControllerTest, AdminRefundControllerTest, AdminExtensionControllerTest, AdminSettingsControllerTest, AdminDashboardControllerTest, AdminAuditLogControllerTest) | 24 | 0 | 0 | 0 | Pass |
| Architecture & Contracts (ArchitectureTest, BookingArchitectureTest, PaymentArchitectureTest, RestApiContractArchitectureTest, ArchitectureFixtureTest, PaymentArchitectureFixtureTest) | 14 | 0 | 0 | 0 | Pass |
| Flyway & Integration (FlywayMigrationTest, FlywayV18LegacyDataTest, FlywayV21PaymentDomainTest, FlywayV22BookingDomainTest, PaymentDomainPersistenceIntegrationTest, AuditLogPersistenceIntegrationTest, AuthRegisterTest, AuthLoginTest, AuthRefreshTokenTest) | 35 | 0 | 0 | 0 | Pass |
| **Full Maven Test Suite (kèm Testcontainers Docker thật)** | **229** | **0** | **0** | **0** | **BUILD SUCCESS** |

---

## 2026-09-04 — Task 6 (Tuần 6) Authorization Facades, Student Session Reports & Clean ArchUnit Stubs

### Đã thực hiện

- **Task 6.1 (Boundary Facades Expansion):**
  - Mở rộng `EnrollmentFacade`: thêm `existsActivePackage(teacherId, studentId)`, `existsActivePackageForPricing(pricingPackageId)`.
  - Mở rộng `BookingEligibilityFacade`: thêm `hasValidBookingOrTrial(teacherId, studentId)`, `getTeacherBookingStats(teacherId)`.
  - Thay thế toàn bộ truy vấn `JdbcTemplate` trực tiếp trong `EnrollmentFacadeImpl` và `BookingEligibilityFacadeImpl` sang Spring Data JPA Repository methods (`StudentPackageRepository`, `BookingRepository`), không làm rò rỉ JPA entity ra ngoài module.
- **Task 6.2 (Student Session Reports API):**
  - Thêm DTO `SessionReportView` (chứa đầy đủ thông tin báo cáo buổi học và thông tin lịch học liên quan).
  - Mở rộng `SessionReportRepository` với query phân trang `findByStudentId(UUID studentId, Pageable pageable)`.
  - Triển khai `BookingService.findStudentSessionReports` thực hiện map theo batch an toàn (không N+1).
  - Triển khai `StudentSessionReportController` (`GET /api/student/session-reports`) với `@RequireRole("STUDENT")` và trả về chuẩn 5-field envelope `ApiResponse.page(...)`.
- **Task 6.3 (Clean ArchUnit Stubs):**
  - Xóa 2 dòng ngoại lệ trong `archunit_ignore_patterns.txt` liên quan đến `EnrollmentFacadeImpl` và `BookingEligibilityFacadeImpl`.
  - Chạy toàn bộ ArchUnit test suite (`ArchitectureTest`, `BookingArchitectureTest`, `PaymentArchitectureTest`, `RestApiContractArchitectureTest`) đạt 100% tuân thủ.
- **Task 6.4 (Full Verification):**
  - Viết Unit tests và Slice tests mới: `EnrollmentFacadeImplTest`, `BookingEligibilityFacadeImplTest`, `StudentSessionReportControllerTest`, `BookingServiceTest`.
  - Chạy full `mvn test` với PostgreSQL Testcontainers: **194/194 tests PASS 100%**.

### Evidence

| Kiểm tra | Run | Failure | Error | Skipped | Kết quả |
|---|---:|---:|---:|---:|---|
| Facade Tests (`EnrollmentFacadeImplTest`, `BookingEligibilityFacadeImplTest`) | 8 | 0 | 0 | 0 | Pass |
| Booking Service & Controller (`BookingServiceTest`, `StudentSessionReportControllerTest`) | 8 | 0 | 0 | 0 | Pass |
| Architecture & Contracts (`BookingArchitectureTest`, `ArchitectureTest`, `PaymentArchitectureTest`, `RestApiContractArchitectureTest`) | 8 | 0 | 0 | 0 | Pass |
| **Full Maven Test Suite (kèm Testcontainers Docker thật)** | **194** | **0** | **0** | **0** | **`BUILD SUCCESS`** |

---

## 2026-09-04 — Sprint 5 Booking, Trial, Session Report và Settlement (TDD)

### Đã thực hiện

- **Gate 5.0 (Migration V22):** Bổ sung `V22__harden_booking_sprint5.sql` và `FlywayV22BookingDomainTest`: partial unique index cho trial booking, unique notification key cho reminder idempotency, batch indexes cho `(status, end_time)` và `(status, expires_at)`, check constraint `teacher_self_rating` 1..5.
- **Gate 5.1 (Domain Models):** Hoàn thiện state machine cho `Booking`, `TrialRequest`, `SessionReport` với các invariant chặt chẽ (start < end, official requires package, trial bans package, cancel reason mandatory, rating range 1..5).
- **Gate 5.2 (Boundary Facades):** Chuẩn hóa `EnrollmentBookingFacade`, `TeacherFacade`, `FinanceFacade`, `CommunicationFacade` và `IdentityFacade` tuân thủ 100% ArchUnit rules, không import chéo entity hay repository.
- **Gate 5.3 (Create Booking):** Triển khai `BookingService.create` & `TeacherBookingController` với thứ tự lock an toàn, kiểm tra overlap lịch, kiểm tra availability slot để phát cảnh báo `outsideAvailabilityWarning` và reserve 1 buổi nguyên tử.
- **Gate 5.4 (Complete & Settlement):** Triển khai `BookingService.complete` tự động tạo `SessionReport`, chuyển `reserved--`, `completed++` cho gói học, tính toán chia học phí giáo viên theo công thức số nguyên tích lũy `floor(...)` tránh lệch số lẻ và hạch toán chuyển tiền ví từ `PENDING` sang `AVAILABLE` qua 2 `LedgerEntry` idempotent.
- **Gate 5.5 (Cancel Booking):** Triển khai `BookingService.cancel` hoàn trả `reserved--`, `remaining++` cho gói học, giữ nguyên bản ghi với `is_deleted = false` phục vụ audit/lịch sử.
- **Gate 5.6 (Trial Requests):** Triển khai `TrialRequestService` & `TrialRequestController` cho phép học sinh gửi yêu cầu học thử (chống trùng lặp pending), giáo viên accept tạo trial booking nguyên tử trong cùng transaction hoặc reject kèm lý do.
- **Gate 5.7 (Student Query):** Triển khai `StudentBookingController` trả danh sách booking phân trang lọc theo `authenticatedUser.id`, không bị lỗi N+1 và đính kèm `SessionReport` khi đã hoàn thành.
- **Gate 5.8 (Schedulers):** Triển khai `BookingReminderJob`, `BookingExpiryJob`, `PackageExpiryJob` được bảo vệ bằng `@SchedulerLock` (ShedLock), tự động expire buổi học quá hạn $\ge 12h$ và gói học quá hạn `expiresAt`.
- **Gate 5.9 (Events & Notifications):** Bắn `BookingEvent` qua `CommunicationFacade` sau khi transaction DB đã commit thành công (`AFTER_COMMIT`).
- **Gate 5.10 (Architecture & Regression):** Toàn bộ bộ test kiến trúc (`BookingArchitectureTest`, `ArchitectureTest`, `PaymentArchitectureTest`, `RestApiContractArchitectureTest`) và unit tests đạt 100% xanh.

### Evidence

| Kiểm tra | Run | Failure | Error | Skipped | Kết quả |
|---|---:|---:|---:|---:|---|
| Booking Domain Tests (`BookingTest`, `TrialRequestTest`, `SessionReportTest`) | 15 | 0 | 0 | 0 | Pass |
| Booking Settlement Formula (`BookingSettlementTest`) | 5 | 0 | 0 | 0 | Pass |
| Booking Jobs (`BookingJobsTest`) | 3 | 0 | 0 | 0 | Pass |
| Booking & Trial Services (`BookingServiceTest`, `TrialRequestServiceTest`) | 10 | 0 | 0 | 0 | Pass |
| Booking & Trial Controllers (`TeacherBookingControllerTest`, `StudentBookingControllerTest`, `TrialRequestControllerTest`) | 7 | 0 | 0 | 0 | Pass |
| Architecture & Contracts (`BookingArchitectureTest`, `ArchitectureTest`, `RestApiContractArchitectureTest`) | 6 | 0 | 0 | 0 | Pass |
| Flyway & Integration (`FlywayMigrationTest`, `FlywayV21PaymentDomainTest`, `FlywayV22BookingDomainTest`, `PaymentDomainPersistenceIntegrationTest`, `AuditLogPersistenceIntegrationTest`) | 12 | 0 | 0 | 0 | Pass |
| **Full Maven Test Suite (kèm Testcontainers Docker thật)** | **185** | **0** | **0** | **0** | **`BUILD SUCCESS`** |

---

## 2026-08-27 — Task 3 Payment-ready Domain Foundation

### Đã thực hiện

- Thêm V20 forward-only: `pgcrypto`, deterministic legacy fingerprint, Invoice idempotency, hai sequence và payment/package/ledger constraints có pre-audit rõ ngữ cảnh.
- Xây `StudentPackage`, `Invoice`, `PaymentTransaction`, `Wallet`, `LedgerEntry` theo scalar ID/snapshot, soft-delete/version hoặc append-only đúng loại dữ liệu.
- Tách Invoice command/query repository custom; mutation Invoice chỉ qua lock + Hibernate dirty checking, không có CRUD `save/update/delete`.
- Thêm canonical UTF-8 fingerprint có amount và URL byte length; golden string/byte/digest tests.
- Mở rộng PricingPackage snapshot với status; thêm `PaymentGateway` port và fake stateful cho reconciliation/timeout/signature/replay/mismatch.
- Cấu hình payment mặc định `disabled`; provider `payos` fail-fast nếu thiếu một trong ba credential. `.env.example` chỉ có placeholder.
- Thêm architecture rules và negative fixtures chặn CRUD Invoice repository, generic mutation, EntityManager trong service và sai command/query boundary.

### Evidence hiện tại

| Kiểm tra | Run | Failure | Error | Skipped | Kết quả |
|---|---:|---:|---:|---:|---|
| Fingerprint + domain + fake gateway + architecture | 18 | 0 | 0 | 0 | Pass |
| Invoice number + provider config + PricingPackage facade | 5 | 0 | 0 | 0 | Pass |
| Main compile | — | 0 | 0 | — | `BUILD SUCCESS` |
| Full suite không phụ thuộc Docker | 94 | 0 | 0 | 0 | Pass |
| V20/Testcontainers runtime gate | 1 | 0 | 1 | 0 | Fail-fast: Docker named pipe không tồn tại |

---

## 2026-08-25 02:28 +07:00 — Tuần 2 Admin Approval & Audit Log

### Đã thực hiện

- Hoàn thành bảy endpoint Admin cho Teacher approval, Subject Proposal approval và User moderation.
- AuditLog immutable/append-only; snapshot whitelist được gom tại `AuditSnapshotMapper`, ghi `JSONB`/`INET` trong cùng transaction với mutation.
- Teacher/Subject/User facade dùng pessimistic lock; double-click/race trả mã `409` riêng và không tạo effect/audit lần hai.
- Subject Proposal hỗ trợ `CREATE_NEW` hoặc `LINK_EXISTING`, đồng thời tạo/restore TeacherSubject qua public facade.
- User moderation chỉ cho `ACTIVE ↔ LOCKED`, cấm self/ADMIN moderation và revoke refresh token khi lock.
- JWT filter kiểm tra current user status qua Redis TTL 30 giây; cache miss/Redis failure fallback DB; status change evict trước mutation và write-through sau commit.
- Đồng bộ endpoint, DTO và HTTP/ErrorCode trong `API_CONTRACT.md` và `ERROR_CODES.md` cùng thay đổi code.

---

## 2026-08-25 01:25 +07:00 — Tuần 1 Foundation runtime verification

### Đã thực hiện

- Gỡ cloud credential/fallback khỏi runtime config; thêm local profile với localhost-only defaults.
- Chuyển Hibernate sang `ddl-auto=validate`, tắt `show-sql` và `open-in-view`.
- Testcontainers fail-closed: không còn `@EnabledIf`, skip khi thiếu Docker hoặc fallback sang database ngoài.
- Xác minh Docker Desktop engine 29.7.2, API 1.55 và Compose 5.4.0; PostgreSQL/Redis Compose đều healthy.
- Ghim Docker API 1.44 cho Testcontainers 1.19.7 để tương thích Docker Engine 29.
- Tạo V18 forward-only và test cho baseline V17, V18 metadata, legacy hợp lệ/không hợp lệ.
- Runtime gate phát hiện V2 đã có `is_deleted` còn V17 thêm `deleted`; V18 hợp nhất hai cờ bằng OR rồi bỏ cột thừa, không mất dữ liệu.
- Sửa schema drift `teacher_profiles.languages` và chuyển entity sang Hibernate 6 native ARRAY mapping để `ddl-auto=validate` chạy thật.
- Chuẩn hóa REST envelope và exception/security responses.
- Thêm PricingPackage/TeacherApproval facade; TeacherStatsJob chỉ gọi facade; sửa soft-delete filter bị thiếu trong Enrollment facade.

---

## 2026-09-04 13:50 +07:00 — Tuần 9: Production Readiness, Security Hardening, Observability, Concurrency Stress & CI/CD Pipeline

### Đã thực hiện
- **Security & IDOR Hardening (Task 9.1):**
  - Xây dựng `SecurityIdorIntegrationTest.java` (11 tests PASS) kiểm chứng toàn diện ma trận phân quyền và IDOR.
  - Chặn triệt để học sinh xem gói/hóa đơn của nhau (404 masked); chặn giáo viên can thiệp booking/tài khoản ngân hàng của giáo viên khác; chặn truy cập trái phép vào tài nguyên Admin.
  - Bảo vệ các endpoint nhạy cảm bằng Redis sliding-window rate limiting.
- **Real-time STOMP/WebSocket Event Bridging (Task 9.2):**
  - Triển khai `TransactionEventWebSocketBridge.java` lắng nghe `@TransactionalEventListener(phase = AFTER_COMMIT)` cho `BookingEvent`, `InvoicePaidEvent`, `PayoutProcessedEvent` để bắn tin nhắn real-time tới queue của người dùng đích.
  - Unit test `TransactionEventWebSocketBridgeTest.java` đạt 100% pass.
- **Observability, Actuator & Micrometer Metrics (Task 9.3):**
  - Tích hợp `spring-boot-starter-actuator` và `micrometer-registry-prometheus`.
  - Mở `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus`.
  - Tạo `PlatformBusinessMetrics.java` đo lường booking, invoice và payout.
  - Test `ActuatorMetricsIntegrationTest.java` pass 100%.
- **Concurrency Stress Test & Performance Benchmarks (Task 9.4):**
  - Viết `ConcurrentStressIntegrationTest.java` (2 tests PASS):
    - 20 luồng đồng thời đặt cùng 1 slot -> Đúng 1 luồng thành công, 19 luồng bị chặn bởi GiST exclusion constraint.
    - 20 luồng đồng thời tạo invoice với cùng Idempotency-Key -> Đúng 1 bản ghi được tạo trong DB và trả về cho cả 20 luồng.
- **CI/CD Pipeline Automation (Task 9.5):**
  - Tạo workflow `.github/workflows/backend-ci.yml` kiểm thử tự động với Testcontainers và build multi-stage Docker image.
- **Toàn bộ Test Suite:**
  - **260/260 tests PASS 100%** (0 failures, 0 errors, 0 skipped). Tất cả 9 tuần phát triển của Backend B đã hoàn tất xuất sắc.