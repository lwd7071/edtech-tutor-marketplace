# Danh sách các lỗi cần khắc phục trong mã nguồn

Dưới đây là danh sách chi tiết các lỗi đã được phát hiện trong mã nguồn trên nhánh `dev` so với các tài liệu quy chuẩn (`API_CONTRACT.md`, `ERROR_CODES.md`, `CODING_CONVENTION.md`, `PLANBE_B.md`). Hãy đánh dấu tick `[x]` vào từng mục sau khi bạn đã khắc phục thành công.

## 1. Các lỗi vi phạm cấu trúc xử lý ngoại lệ (Exception Handling)

- [x] **Lỗi 1.1: Bỏ sót mã lỗi `FILE_TOO_LARGE` đối với file vượt quá dung lượng**
  - **Mô tả chi tiết:** Trong `GlobalExceptionHandler.java`, hàm xử lý lỗi upload file (cho `MaxUploadSizeExceededException`) đang gán cứng về `ErrorCode.VALIDATION_ERROR` (HTTP 400). Điều này vi phạm `ERROR_CODES.md` vì tài liệu đã định nghĩa rõ mã lỗi `FILE_TOO_LARGE` (HTTP 413) cho trường hợp "Dung lượng file vượt giới hạn".
  - **Tác hại:** Client sẽ nhận lỗi 400 chung chung thay vì 413 báo file quá lớn như API Contract hứa hẹn.

- [x] **Lỗi 1.2: Nuốt mất "Global Errors" trong Bean Validation**
  - **Mô tả chi tiết:** Hàm `handleValidationException` xử lý `MethodArgumentNotValidException` chỉ quét qua `getFieldErrors()` và bỏ qua hoàn toàn `getGlobalErrors()`. Trong khi đó, `API_CONTRACT.md` yêu cầu "Lỗi domain có `field=null` hiển thị thông báo form/toast phù hợp", nghĩa là các lỗi chung (cross-field validation) cũng phải được trả về.
  - **Tác hại:** Các lỗi validation liên quan đến nhiều trường (ví dụ `startTime < endTime` kiểm tra ở cấp class) bị mất hoàn toàn, không trả về cho client.

- [x] **Lỗi 1.3: Log lỗi 500 (Unhandled Exception) nhưng thiếu Stack Trace**
  - **Mô tả chi tiết:** Hàm `handleException` bắt các lỗi không xác định (Exception.class) có ghi log nhưng lại viết thiếu biến `ex` ở cuối hàm log. Đoạn code hiện tại là: `log.error("Unhandled exception type={}, requestId={}", ex.getClass().getName(), getRequestId());`. Đáng lý ra phải truyền object `ex` vào cuối để log in ra được toàn bộ stack trace.
  - **Tác hại:** Không lưu lại stack trace ở server, gây cản trở và làm chậm trễ quá trình debug khi hệ thống gặp lỗi nghiêm trọng (Internal Server Error).

- [x] **Lỗi 1.4: Bóp méo HTTP Status của `ResponseStatusException`**
  - **Mô tả chi tiết:** Trong hàm `handleResponseStatusException`, code ép buộc mọi ngoại lệ (nếu không phải 404 NOT_FOUND) biến thành `INTERNAL_SERVER_ERROR` (500).
  - **Tác hại:** Điều này quá cứng nhắc. Khi Spring framework ném ra lỗi 400 (Bad Request) hay 405 (Method Not Allowed) qua ResponseStatusException, status code thực tế sẽ bị làm mất và thay bằng 500, gây nhiễu cho việc monitor hệ thống.

## 2. Các lỗi vi phạm quy tắc thiết kế và kiến trúc (Architecture & Pattern)

- [x] **Lỗi 2.1: `ErrorCode` enum có đầy đủ nhưng không dùng — ném thẳng `ResponseStatusException`**
  - **Mô tả chi tiết:** Code trong `ReviewService.java` ném `ResponseStatusException` (vd: `throw new ResponseStatusException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS");`) thay vì dùng `throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS)`.
  - **Tác hại:** Việc ném lỗi không qua `BusinessException` sẽ phá vỡ format Error Response chuẩn, làm cho quá trình xử lý ngoại lệ bị phân mảnh.

- [x] **Lỗi 2.2: Controller inject Repository trực tiếp (Vi phạm kiến trúc phân tầng)**
  - **Mô tả chi tiết:** `ConversationController` đang inject trực tiếp `ConversationRepository` và `MessageRepository`. Tương tự, `PublicTeacherController` inject thẳng `PricingPackageRepository`.
  - **Tác hại:** Phá vỡ kiến trúc 3 lớp (Controller -> Service/Facade -> Repository). Controller đáng lẽ chỉ được xử lý Request/Response và gọi xuống Service layer.

- [x] **Lỗi 2.3: Vi phạm ranh giới Module bằng Raw SQL khổng lồ (Nối chéo 7 bảng)**
  - **Mô tả chi tiết:** Trong `TeacherMarketplaceService.java` (hàm `searchTeachers` và `getTeacherDetail`), có các truy vấn raw SQL nối trực tiếp 7 bảng bao gồm: `teacher_profiles`, `users`, `teacher_stats`, `teacher_subjects`, `subjects`, `pricing_packages`, và `teacher_availabilities`.
  - **Tác hại:** Vi phạm quy tắc chia tách domain trong Monolith ("đã fix" trong tài liệu). Module này đáng lý không được trực tiếp query vào bảng của các module khác (như ranking, catalog) mà không thông qua Facade interface.

- [x] **Lỗi 2.4: Entity không kế thừa `BaseEntity` (Bỏ qua Convention)**
  - **Mô tả chi tiết:** Hàng loạt các Entity như `TeacherStats.java`, `Review.java`, `TeacherProfile.java`, `TeacherSubject.java`, v.v... được đánh dấu `@Entity` nhưng hoàn toàn không dùng cú pháp `extends BaseEntity`.
  - **Tác hại:** Làm mã nguồn thiếu nhất quán, các field common như id, createdAt, updatedAt bị định nghĩa phân tán hoặc bỏ sót.

- [x] **Lỗi 2.5: Nuốt mất ngoại lệ (Swallow Exception) trong hàm `@Transactional`**
  - **Mô tả chi tiết:** Trong `PricingPackageService.java` (dòng 136, thuộc hàm `toView()`), mã nguồn có khối `catch (Exception e) { log.warn(...); }` để bắt và dập tắt lỗi (không throw lại). Lỗi tương tự xuất hiện trong `TeacherStatsService`, `TeacherFacadeImpl`, và `TeacherStatsJob`. Vì các hàm cha đang chạy trong giao dịch (`@Transactional`), việc bắt và nuốt lỗi sẽ dẫn đến hậu quả rất xấu.
  - **Tác hại:** Nuốt mất ngoại lệ khiến cho Spring không thể kích hoạt cơ chế rollback của Transaction khi xảy ra các lỗi nghiệp vụ nghiêm trọng. Dữ liệu rác có thể bị commit vào database.

## 3. Các lỗi vi phạm API Contract và Testing

- [x] **Lỗi 3.1: DTO trả về thiếu field, sai kiểu dữ liệu so với `API_CONTRACT.md`**
  - **Mô tả chi tiết:** DTO `TeacherCard.java` hoàn toàn khác biệt so với mô tả trong `API_CONTRACT.md`:
    - Code dùng `List<String> subjects`, nhưng contract yêu cầu mảng object `[{ id, name }]`.
    - Code dùng `minPriceVnd`, nhưng contract ghi `startingPriceVnd`.
    - Code dùng `bio`, nhưng contract ghi `bioExcerpt`.
    - Thiếu hoàn toàn các trường cờ quan trọng như `verifiedBadge`, `supportsOnline`, `supportsOffline`.
  - **Tác hại:** Gây lỗi hiển thị trên UI ở phía Frontend, làm sai lệch kết quả trả về của API.

- [x] **Lỗi 3.2: 0 test cho module và 0 controller test (Báo cáo sai tiến độ)**
  - **Mô tả chi tiết:** Không tồn tại bất kỳ một file Controller Test nào trong `src/test/java` (tất cả các module). Ngoài ra, các module như `booking`, `payment`, `finance`, `admin`, `scheduler` hoàn toàn không có thư mục test. Mặc dù vậy, file báo cáo `PROGRESS_BE_B.md` vẫn đánh dấu là "Done".
  - **Tác hại:** Không đảm bảo tính đúng đắn của logic, tiềm ẩn rủi ro rất cao trong các phần cốt lõi chưa được cover test.

- [x] **Lỗi 3.3: Skip 8 Test nhưng vẫn ghi nhận "Done"**
  - **Mô tả chi tiết:** Quá trình chạy test thực tế cho thấy có 8 test bị skip (do phụ thuộc Docker Testcontainers), nhưng báo cáo (`PROGRESS_BE_B.md`) lại gian lận ghi kết quả là `0 skipped` nhằm vượt qua Definition of Done.
  - **Tác hại:** Bỏ sót các đợt kiểm tra tự động quan trọng cho database migration (V1-V18), làm ảnh hưởng tới độ tin cậy của toàn bộ nền tảng.
