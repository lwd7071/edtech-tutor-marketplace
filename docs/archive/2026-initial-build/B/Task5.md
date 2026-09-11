# Kế hoạch & Bàn giao — Task 5 (Tuần 5): Booking, Trial, Session Report & Settlement

## Tổng quan (Handoff Summary)
Tài liệu này ghi chú lại chi tiết tiến độ và kết quả thực hiện của Task 5 (Sprint 5) theo phương pháp TDD.

- **Trạng thái:** Đã hoàn thành (Done)
- **Kiểm thử:** 100% test xanh trên Testcontainers PostgreSQL 16 & Redis 7.
- **Tuân thủ:** ArchUnit, Không vi phạm boundary module, Clean DDD.

---

## Chi tiết những gì đã làm

### 1. Migration & Schema Hardening (V22)
- Tạo `V22__harden_booking_sprint5.sql` forward-only:
  - Partial unique index `ux_bookings_trial_pair`: tối đa 1 trial booking `SCHEDULED` hoặc `COMPLETED` cho mỗi cặp student-teacher.
  - Unique notification idempotency index `ux_notifications_idempotency` chống gửi trùng reminder.
  - Batch query indexes cho `bookings(status, end_time)` và `student_packages(status, expires_at)`.
  - Check constraint `ck_session_reports_rating` (1..5 sao).
- Test: `FlywayV22BookingDomainTest.java` xác nhận migration chạy mượt mà từ DB rỗng.

### 2. Domain Models & State Machine
- **`Booking`**:
  - Quản lý trạng thái `SCHEDULED` $\rightarrow$ `COMPLETED` / `CANCELLED` / `EXPIRED`.
  - Invariants: `startTime < endTime`, official booking bắt buộc có `studentPackageId`, trial booking cấm `studentPackageId`.
  - Cancel reason bắt buộc; `cancelInitiatedBy` thuộc (`TEACHER`, `STUDENT_REQUEST`, `SYSTEM`).
  - Hủy booking giữ nguyên bản ghi với `is_deleted = false` để bảo toàn lịch sử và audit.
- **`TrialRequest`**:
  - Trạng thái `PENDING` $\rightarrow$ `ACCEPTED` / `REJECTED` / `CANCELLED`.
  - Tối đa 1 request `PENDING` cho mỗi cặp student-teacher.
- **`SessionReport`**:
  - Gắn 1-1 với `Booking`.
  - Đánh giá `teacherSelfRating` từ 1 đến 5 sao.

### 3. Service Layer & Concurrency Control
- **Create Booking**:
  - Lock thứ tự an toàn: `TeacherProfile` $\rightarrow$ `User` $\rightarrow$ `StudentPackage (FOR UPDATE)`.
  - Kiểm tra `package.remainingSessions > 0`, `booking.endTime <= package.expiresAt`.
  - Chuyển đổi atomic counter: `remainingSessions--`, `reservedSessions++`.
  - GiST exclusion index `ex_booking_teacher_overlap` và `ex_booking_student_overlap` là lớp phòng thủ cuối cùng chống trùng lịch.
  - Cảnh báo `outsideAvailabilityWarning = true` nếu nằm ngoài khung giờ rảnh của giáo viên nhưng vẫn cho phép đặt.
- **Complete Booking & Settlement**:
  - Chuyển `reservedSessions--`, `completedSessions++`.
  - Tự động tạo `SessionReport` trong cùng transaction.
  - Phân bổ học phí giáo viên theo công thức làm tròn tích lũy $\lfloor\dots\rfloor$ (zero-residual rounding):
    $$\text{sessionAmount} = \lfloor \frac{(\text{resolvedBefore} + 1) \times \text{price}}{\text{total}} \rfloor - \lfloor \frac{\text{resolvedBefore} \times \text{price}}{\text{total}} \rfloor$$
  - Hạch toán ví: chuyển tiền từ `PENDING` sang `AVAILABLE` và ghi nhận 2 `LedgerEntry` idempotent.
- **Cancel Booking**:
  - Chuyển `reservedSessions--`, `remainingSessions++`.
  - Giải phóng slot thời gian để giáo viên có thể nhận lịch mới.

### 4. Background Schedulers (ShedLock)
- `BookingReminderJob`: Quét các buổi học sắp diễn ra sau $X$ giờ (cấu hình trong `platform_settings`) và gửi notification idempotent.
- `BookingExpiryJob`: Tự động chuyển các buổi học `SCHEDULED` quá hạn $\ge 12h$ sang `EXPIRED`, hoàn trả counter `reserved--`, `remaining++`.
- `PackageExpiryJob`: Tự động khóa các gói học quá hạn `expiresAt` sang `LOCKED_EXPIRED`.

---

## Bằng chứng kiểm thử
- `BookingTest`, `TrialRequestTest`, `SessionReportTest`: 15 tests PASS.
- `BookingSettlementTest`: 5 tests PASS.
- `BookingJobsTest`: 3 tests PASS.
- `TeacherBookingControllerTest`, `StudentBookingControllerTest`, `TrialRequestControllerTest`: 15 tests PASS.
- `BookingArchitectureTest`: PASS 100%.

