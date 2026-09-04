# Kế hoạch & Bàn giao — Task 8 (Tuần 8): Hardening & Testing, Demo Seed Data Migration, Dockerization & Final Verification

## Tổng quan (Handoff Summary)
Tài liệu này ghi chú lại chi tiết tiến độ và kết quả thực hiện của Task 8: Kiểm chứng toàn diện 7 Invariants cốt lõi của hệ thống, Tạo migration dữ liệu mẫu demo V23, Đóng gói Docker multi-stage & Docker Compose, Chạy full verification test suite.

- **Trạng thái:** Đã hoàn thành (Done)
- **Kiểm thử:** 240/240 tests PASS 100% (0 fail, 0 skip) trên PostgreSQL 16 & Redis 7 Testcontainers Docker thật.
- **Docker:** Đã tạo `backend/Dockerfile` multi-stage, `backend/.dockerignore`, cập nhật `docker-compose.yml`.

---

## Chi tiết những gì đã làm

### 1. Hardening Invariants Integration Test (Task 8.1)
Tạo `HardeningInvariantsIntegrationTest.java` kiểm chứng 7 invariants nền tảng:
1. **Ledger Integrity**: Tổng các bút toán sổ cái theo từng bucket (`PENDING`, `AVAILABLE`, `RESERVED`) luôn khớp chính xác 100% với số dư trong bảng `wallets`.
2. **Package Counter Invariant**: `remaining_sessions + reserved_sessions + completed_sessions + refunded_sessions = total_sessions`. Mọi vi phạm đều bị chặn ở cả mức database constraint lẫn domain method.
3. **GiST Exclusion (No Double-booking)**: Database ném `PSQLException` với constraint `ex_booking_teacher_overlap` khi cố tình chèn 2 booking giao nhau thời gian của cùng một giáo viên.
4. **Booking Cancellation & Slot Release**: Hủy booking cập nhật trạng thái `CANCELLED`, giữ `is_deleted = false` (phục vụ audit/lịch sử), đồng thời giải phóng slot thời gian để giáo viên nhận lịch mới.
5. **Cumulative Rounding Precision**: Thuật toán làm tròn tích lũy $\lfloor\dots\rfloor$ đảm bảo hạch toán học phí và hoàn tiền không thất thoát 1 đồng lẻ nào (zero residual penny loss).
6. **Soft Delete & Optimistic Lock Versioning**: Xóa mềm cập nhật `is_deleted = true`, ẩn khỏi truy vấn JPA nhờ `@SQLRestriction("is_deleted = false")`, tự động tăng `version` khi có thay đổi.
7. **Append-Only Immutability**: Các bảng `ledger_entries`, `payment_transactions`, `audit_logs` được xác nhận không có cột `is_deleted` hay `updated_at`, ngăn chặn sửa/xóa dữ liệu lịch sử.

### 2. Demo Seed Data Migration (Task 8.2)
- Tạo migration `V23__seed_demo_data.sql`:
  - 1 Quản trị viên hệ thống: `admin@edtech.vn`.
  - 3 Giáo viên tiêu biểu (Toán, Tiếng Anh, Vật Lý) có hồ sơ `APPROVED`, hiển thị công khai.
  - 3 Môn học chuẩn (`SUB_MATH`, `SUB_ENG`, `SUB_PHY`).
  - 4 Gói học phí `ACTIVE` với giá từ 1.900.000 đến 4.200.000 VNĐ.
  - 3 Học sinh `ACTIVE`.
  - 2 Hóa đơn `PAID` kèm mã PayOS và request fingerprint SHA-256.
  - 2 Gói học sinh sở hữu `ACTIVE` với số counter chuẩn xác.
  - 2 Buổi học (1 `COMPLETED` kèm `SessionReport` & hạch toán ví, 1 `SCHEDULED`).
  - 2 Tài khoản ngân hàng giáo viên mã hóa AES-GCM và ví giáo viên có 4 bút toán `LedgerEntry` khớp từng đồng.
- Test: `FlywayV23DemoDataTest` và `FlywayMigrationTest` (23/23 migration xanh 100%).

### 3. Dockerization & Deployment (Task 8.3)
- **`backend/Dockerfile`**:
  - Stage 1 (Builder): Eclipse Temurin 21 JDK Alpine, Maven build JAR và trích xuất layer bằng Spring Boot `layertools`.
  - Stage 2 (Runner): Eclipse Temurin 21 JRE Alpine, tạo user không đặc quyền `appuser`, sao chép layer và chạy qua `JarLauncher`.
  - JVM Flags: `-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom`.
- **`backend/.dockerignore`**: Loại trừ `target/`, `.git/`, `.idea/`, `*.log`, `.env`.
- **`docker-compose.yml`**: Bổ sung service `backend` kết nối `postgres` và `redis` với healthcheck dependencies.

---

## Bằng chứng kiểm thử
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 240, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:16 min
[INFO] Finished at: 2026-09-04T12:59:17+07:00
[INFO] ------------------------------------------------------------------------
```

