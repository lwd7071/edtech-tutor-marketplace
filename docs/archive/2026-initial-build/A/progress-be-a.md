# Progress BE – thành viên A

Ngày thực hiện: 2026-08-27

## Phạm vi

Đối chiếu `docs/A/review_report.md` (phần 1, 23 mục) với mã nguồn backend và migration. Chỉ sửa các lỗi thuộc BE A; không thay đổi quyền truy cập hay dữ liệu runtime hiện có.

## Quy trình diagnose

1. Reproduce bằng tìm kiếm tĩnh (`rg`) các annotation, import, error code, truy vấn native, pattern gọi facade trong vòng lặp và cấu hình CORS.
2. Giả thuyết xếp hạng trước khi sửa:
   - H1 (cao): nhiều lỗi có chung nguyên nhân là các entity/soft-delete chưa đồng bộ.
   - H2 (cao): tài liệu hoàn thành trước code nên còn lệch trạng thái và tên constraint.
   - H3 (trung bình): lỗi runtime nằm trong native SQL/migration, không phát hiện qua kiểm tra import.
   - H4 (trung bình): các facade gọi từng bản ghi gây N+1 dù unit test vẫn có thể pass.
3. Instrument/feedback loop: kiểm tra diff, grep lại các pattern lỗi, kiểm tra quan hệ migration–entity và chạy build/test nếu môi trường cho phép.

## Các thay đổi đã thực hiện

- `TeacherStats.calculatedAt` và `TeacherStatsView.calculatedAt` thống nhất về `Instant`; bổ sung import `UpdateTimestamp`.
- Thay toàn bộ `@Where` deprecated ở các entity A bằng `@SQLRestriction`.
- `Conversation`, `Message`, `Notification` kế thừa `BaseEntity`; Message/Notification có soft-delete filter. Thêm migration V20 cho `notifications.updated_at` và `notifications.is_deleted`.
- Xóa import chết (`ResponseStatusException`, `HttpStatus`, EntityManager, JPA imports của Assignment) và import UUID trùng.
- Bỏ điều kiện soft-delete trùng trong JPQL ReviewRepository.
- Bổ sung điều kiện subject active/deleted trong batch search và global ranking native SQL.
- Thêm bulk methods cho Subject/Identity/Teacher facade; loại bỏ N+1 ở chi tiết giáo viên và bảng xếp hạng.
- Thêm error code nghiệp vụ `TEACHER_STATS_NOT_FOUND`, `STUDENT_NOT_FOUND`, `LEARNING_RELATIONSHIP_NOT_FOUND` và dùng đúng tại service.
- Sửa mapping tên unique constraint subject trong GlobalExceptionHandler theo migration (`uq_subjects_code`, `uq_subjects_slug`).
- CORS fail-closed khi cấu hình wildcard; chỉ chấp nhận origin tường minh.

## 23 mục trong báo cáo

Mục 1–7, 9–18 và 23 đã được xử lý trong code. Mục 8, 19–22 là sai lệch tài liệu; tài liệu cũ `finished.md`, `finished-2.md`, `finishedn+1.md`, `PROGRESS_BE_A.md` không còn tồn tại trong workspace hiện tại, nên trạng thái chuẩn được ghi tại file này. `Submission` vẫn giữ `@ManyToOne Assignment` vì đây là liên kết nội bộ cùng module learning; tuyên bố “toàn bộ reference đã thành UUID” trong tài liệu cũ là không chính xác và không nên áp dụng máy móc.

## Kiểm tra hồi quy

- `git diff --check`: không phát hiện lỗi whitespace do các thay đổi mới.
- `rg` xác nhận không còn `@Where` trong entity A, không còn import chết được báo cáo, không còn wildcard CORS được chấp nhận âm thầm, và các facade bulk đã được sử dụng.
- Đã kiểm tra migration V13/V20 để bảo đảm các cột soft-delete/timestamp tồn tại.
- Đã thử `mvn -o ... compile` bằng Maven/JDK bundled; Maven dừng trước bước compile vì repository offline thiếu `spring-boot-starter-parent:3.2.5`. Lần thử trước với JDK bundled mặc định cũng lỗi `lib/jvm.cfg`. Vì vậy chưa thể khẳng định test tích hợp/database đã pass; cần chạy lại `mvn clean verify` trong môi trường có Maven repository đầy đủ (có network/cache hợp lệ).

## Kết luận

Các lỗi phần 1 đã được xử lý theo nhóm nguyên nhân và có kiểm tra tĩnh sau sửa. Rủi ro còn lại chỉ là xác nhận compile/test tích hợp trong môi trường Maven đầy đủ, đặc biệt migration V20 và các test mock facade mới.

## Tiếp tục xử lý Phần 2 (17 lỗi)

### Đã sửa

- Đồng bộ `TeacherSearchParams` với API contract bằng cách bỏ `educationLevel` khỏi request và truy vấn catalog.
- Bổ sung `/api/auth/oauth2/exchange` vào bảng API contract vì endpoint phục vụ luồng OAuth hiện hữu.
- Dọn import thừa của `Review`, đổi `isVisible` từ `Boolean` sang primitive `boolean` để loại bỏ nguy cơ NPE.
- Bổ sung role `STUDENT/TEACHER` trên từng method của `ConversationController`; đồng thời giới hạn `/api/student/notifications/**` bằng role `STUDENT` trong SecurityConfig.
- Thay wildcard import ở catalog bằng danh sách import tường minh.
- Chuyển các response DTO thuộc phạm vi A sang Lombok `@Value` (immutable) nhưng giữ `ContentBlock` mutable vì nó được Jackson bind trực tiếp từ request DTO.
- Biến `PublicReviewController` thành marker deprecated không đăng ký Spring, tránh trùng endpoint với `ranking.ReviewController`.
- Đổi `TestEventPublisherController` sang profile `test`, không còn được expose ở profile mặc định/production.
- Xóa import entity khỏi `ConversationController`.
- Đưa luồng availability qua `TeacherFacade.getPublicAvailability`, loại bỏ catalog gọi trực tiếp `TeacherAvailabilityService`.
- Ghi nhận endpoint OAuth exchange trong `API_CONTRACT.md` và bổ sung ba error code mới vào `ERROR_CODES.md` từ phần 1.
- Các DTO đã dùng `ApiResponse.created` đều có `@ResponseStatus(HttpStatus.CREATED)` ở controller; không thay đổi factory vì factory chỉ tạo envelope, status thuộc HTTP layer.
- Cập nhật ERD `TEACHER_STATS` theo migration V19 (`id` là PK, `teacher_id` unique và có các cột BaseEntity), loại bỏ lệch tài liệu.

### Các điểm đối chiếu ERD/plan

- `TeacherStats` dùng khóa `id` + unique `teacher_id` theo migration V19 hiện hành; ERD cũ ghi `teacher_id` là PK nên đã được ghi nhận là tài liệu lệch schema, không đổi ngược entity gây mất tương thích dữ liệu.
- `catalog` đã giao tiếp với `teacher` qua `TeacherFacade`; availability DTO vẫn thuộc public facade contract.
- Quy tắc immutable DTO được áp dụng cho response; các request model cần setter/no-args để Jackson deserialize vẫn giữ mutable có chủ đích.

### Kiểm tra sau Phần 2

- `rg` không còn `import java.util.*`, không còn `educationLevel()` trên `TeacherSearchParams`, không còn `@Where`, và role path notification đã có rule cụ thể.
- Kiểm tra tất cả nơi gọi `new TeacherSearchParams` sau khi bỏ field; test constructor đã được cập nhật.
- Kiểm tra endpoint review chỉ còn một implementation hoạt động (`ReviewController`).
- Maven compile vẫn bị chặn trước compile do dependency parent chưa có trong local repository offline; cần chạy `mvn clean verify` trong môi trường có cache/network Maven hợp lệ.

## Kết luận review cuối cùng (TDD + Diagnose)

### Feedback loop và kết quả

- RED/reproduce: `mvn test` và `mvn compile` được chạy với Maven/JDK bundled; cả hai dừng trước compile vì offline repository thiếu `spring-boot-starter-parent:3.2.5`, không phải do một assertion test cụ thể.
- Static regression loop: không còn wildcard import, `@Where`, field `educationLevel` trong catalog search, import entity ở ConversationController, hoặc controller review trùng endpoint.
- Contract/schema loop: OAuth exchange đã được ghi trong API contract; ERD TeacherStats đã khớp migration V19; error codes mới đã ghi trong `ERROR_CODES.md`; CORS/student notification/Conversation role đã có guard.
- TDD seam: các controller/service test hiện có được giữ ở public interface; đã cập nhật `TeacherSearchParams` test và `IdentitySnapshot` test fixture theo API hiện tại. Chưa thêm test integration mới vì không thể khởi động Maven/Testcontainers trong môi trường thiếu dependency.

### Đánh giá tối ưu/đồng bộ

Trong phạm vi BE thành viên A, các lỗi đã nêu trong phần 1 (23) và phần 2 (17) đã được xử lý hoặc đối chiếu tài liệu tương ứng. Không thể tuyên bố tuyệt đối “100% không lỗi” khi chưa chạy được compile/integration test thật; rủi ro còn lại là lỗi biên dịch/runtime chỉ xuất hiện khi Maven tải đủ dependency, Hibernate khởi tạo mapping, Flyway chạy V20 và Jackson bind DTO.

### Hypothesis/post-mortem

- H1/H2 đúng một phần: phần lớn lỗi đến từ lệch entity–migration–tài liệu và boundary API.
- H3 đúng: build bị chặn bởi dependency cache, nên static checks không thay thế được integration build.
- H4 đúng: bulk facade loại bỏ các vòng gọi facade trong ranking/detail; cần benchmark SQL thực tế sau khi có database.
- Không còn debug instrumentation `[DEBUG-*]` hoặc prototype tạm trong các thay đổi của lượt review này.

Khuyến nghị cuối: chạy `mvn clean verify` (kèm PostgreSQL/Testcontainers) trong CI hoặc máy có Maven Central/cache đầy đủ trước khi merge; coi đó là điều kiện xác nhận cuối cùng, không phải lỗi code đã được chứng minh trong workspace hiện tại.
