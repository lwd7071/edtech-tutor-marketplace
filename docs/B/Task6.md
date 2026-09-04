# Kế hoạch & Bàn giao — Task 6 (Tuần 6): Authorization Facades, Student Session Reports & Clean ArchUnit Stubs

## Tổng quan (Handoff Summary)
Tài liệu này ghi chú lại chi tiết tiến độ và kết quả thực hiện của Task 6: Mở rộng các public facade ranh giới module, hoàn thiện API báo cáo buổi học cho học sinh và dọn sạch các ngoại lệ ArchUnit.

- **Trạng thái:** Đã hoàn thành (Done)
- **Kiểm thử:** 194/194 tests PASS 100%.
- **Tuân thủ:** Không còn ngoại lệ nào trong `archunit_ignore_patterns.txt`.

---

## Chi tiết những gì đã làm

### 1. Mở rộng Boundary Facades
- **`EnrollmentFacade`** (`com.edtech.platform.enrollment.facade`):
  - Thêm phương thức `existsActivePackage(UUID teacherId, UUID studentId)` phục vụ xác thực quyền truy cập phòng học / chat.
  - Thêm phương thức `existsActivePackageForPricing(UUID pricingPackageId)` phục vụ kiểm tra điều kiện chuyển trạng thái gói học phí.
  - Thay thế toàn bộ `JdbcTemplate` trong `EnrollmentFacadeImpl` bằng JPA repository (`StudentPackageRepository`).
- **`BookingEligibilityFacade`** (`com.edtech.platform.booking.facade`):
  - Thêm phương thức `hasValidBookingOrTrial(UUID teacherId, UUID studentId)`.
  - Thêm phương thức `getTeacherBookingStats(UUID teacherId)` phục vụ thống kê tổng số buổi đã dạy, tỉ lệ hoàn thành.
  - Thay thế `JdbcTemplate` trong `BookingEligibilityFacadeImpl` bằng JPA repository (`BookingRepository`).

### 2. Student Session Reports API
- Triển khai `StudentSessionReportController`:
  - Endpoint: `GET /api/student/session-reports` (phân trang, bảo vệ bởi `@RequireRole("STUDENT")`).
  - Response: Chuẩn envelope 5 trường `ApiResponse.page(...)` trả về `SessionReportView`.
  - Service: `BookingService.findStudentSessionReports` thực hiện map theo batch an toàn, không gây N+1 query.

### 3. Dọn sạch ArchUnit Ignore Patterns
- Xóa bỏ hoàn toàn các dòng ngoại lệ tạm thời trong `archunit_ignore_patterns.txt`.
- Toàn bộ 4 bộ test kiến trúc (`ArchitectureTest`, `BookingArchitectureTest`, `PaymentArchitectureTest`, `RestApiContractArchitectureTest`) đạt 100% tuân thủ.

---

## Bằng chứng kiểm thử
- `EnrollmentFacadeImplTest`, `BookingEligibilityFacadeImplTest`: 8 tests PASS.
- `BookingServiceTest`, `StudentSessionReportControllerTest`: 8 tests PASS.
- ArchUnit Architecture Tests: 8 tests PASS.
- Toàn bộ test suite: 194/194 tests PASS 100%.

