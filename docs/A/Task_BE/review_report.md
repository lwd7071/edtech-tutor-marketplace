# 📋 Báo cáo Review: Code vs Tài liệu MD

> Review đối chiếu toàn bộ code thực tế với 6 file MD trong `docs/A/`
> Ngày review: 2026-08-27

---

## TỔNG QUAN

Đã đọc và đối chiếu 6 file MD:
- [finishedn+1.md](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/A/Task_A/finishedn+1.md)
- [finished.md](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/A/Task_A/finished.md)
- [finished-2.md](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/A/Task_A/finished-2.md)
- [ketluan.md](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/A/Task_A/ketluan.md)
- [tasknew.md](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/A/tasknew.md)
- [PROGRESS_BE_A.md](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/A/PROGRESS_BE_A.md)

---

## 🔴 LỖI NGHIÊM TRỌNG (Compile/Runtime)

### Lỗi 1: `TeacherStats.java` — Thiếu import `@UpdateTimestamp`

- **File**: [TeacherStats.java:65](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/domain/TeacherStats.java#L65)
- **Mô tả**: Dòng 65 sử dụng `@UpdateTimestamp` nhưng **không có dòng import** tương ứng (`import org.hibernate.annotations.UpdateTimestamp`). Đã grep xác nhận không có import nào trong file.
- **MD liên quan**: `finishedn+1.md` mục 9 tuyên bố "Đã thêm lại `import org.springframework.stereotype.Service` bị tuột mất trong `TeacherStatsService`" nhưng **không đề cập** đến lỗi thiếu import `@UpdateTimestamp` trong entity `TeacherStats`.
- **Hệ quả**: **Lỗi compile**. File sẽ không biên dịch được.

### Lỗi 2: `TeacherStats.java` — Kiểu thời gian không nhất quán với `BaseEntity`

- **File**: [TeacherStats.java:66](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/domain/TeacherStats.java#L66)
- **Mô tả**: `calculatedAt` dùng kiểu `ZonedDateTime` trong khi `BaseEntity` sử dụng `Instant` cho `createdAt`/`updatedAt`. `TeacherStats` kế thừa `BaseEntity` nên đã có sẵn `updatedAt` từ `@PreUpdate` — việc thêm `@UpdateTimestamp` trùng lặp trên một field khác (`calculatedAt`) có thể gây conflict với JPA Auditing.
- **MD liên quan**: `finished-2.md` mục 2.4 tuyên bố "Đồng bộ hóa kiểu dữ liệu thời gian (`Instant`)" nhưng thực tế `calculatedAt` vẫn là `ZonedDateTime`.

---

## 🟠 LỖI KHÔNG ĐỒNG BỘ GIỮA MD VÀ CODE

### Lỗi 3: `Review.java` — Sử dụng `@Where` (deprecated) thay vì `@SQLRestriction`

- **File**: [Review.java:30](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/domain/Review.java#L30)
- **Mô tả**: `Review.java` dùng `@Where(clause = "is_deleted = false")` — annotation cũ, bị deprecated từ Hibernate 6.3+ và nên thay bằng `@SQLRestriction`.
- **MD liên quan**: `finishedn+1.md` mục 1 tuyên bố "Đã đồng bộ hóa thêm `@SQLDelete` và `@SQLRestriction(\"is_deleted = false\")` cho 7 Entity cốt lõi". Nhưng `Review` vẫn dùng `@Where`, không phải `@SQLRestriction`.
- **Các entity khác dùng `@Where` thay vì `@SQLRestriction`**: `PricingPackage.java`, `TeacherProfile.java`, `TeacherDocument.java`, `TeacherSubject.java`, `Subject.java`, `SubjectProposal.java` — tất cả đều dùng `@Where`. Chỉ có `User`, `RefreshToken`, `Attachment`, `Assignment`, `Submission`, `Conversation`, `TeacherAvailability`, `TeacherStats` dùng `@SQLRestriction`. **Không đồng nhất**.

### Lỗi 4: `Conversation.java` — Không kế thừa `BaseEntity`

- **File**: [Conversation.java:28](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/communication/domain/Conversation.java#L28)
- **Mô tả**: `Conversation` **không** `extends BaseEntity`, tự quản lý `id`, `createdAt`, `updatedAt`, `isDeleted` thủ công.
- **MD liên quan**: `finished-2.md` mục 2.4 tuyên bố "Kế thừa `BaseEntity` cho các entity này." và `ketluan.md` P0 cũng nêu vấn đề "Conversation không kế thừa BaseEntity". Nhưng thực tế **vẫn chưa fix**.

### Lỗi 5: `Message.java` — Không kế thừa `BaseEntity`, không có `@SQLDelete`/`@SQLRestriction`

- **File**: [Message.java:26](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/communication/domain/Message.java#L26)
- **Mô tả**: `Message` **không** `extends BaseEntity`, tự quản lý `id`, `createdAt`, `updatedAt`, `isDeleted`. Không có `@SQLDelete` hay `@SQLRestriction`, nghĩa là `findById()` vẫn trả về record đã xóa mềm.
- **MD liên quan**: `finishedn+1.md` mục 1 liệt kê 7 entity đã bổ sung soft-delete nhưng **không** bao gồm `Message`.

### Lỗi 6: `Notification.java` — Không kế thừa `BaseEntity`, không có soft-delete

- **File**: [Notification.java:23](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/communication/domain/Notification.java#L23)
- **Mô tả**: `Notification` **không** `extends BaseEntity`, không có `isDeleted`, `@SQLDelete`, `@SQLRestriction`. Thậm chí không có `updatedAt`.
- **MD liên quan**: Không có MD nào đề cập việc fix `Notification`. `ketluan.md` liệt kê vấn đề soft-delete cho các entity mutable nhưng `Notification` bị bỏ sót.

### Lỗi 7: `ReviewService.java` — Vẫn import `ResponseStatusException`

- **File**: [ReviewService.java:15](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/service/ReviewService.java#L15)
- **Mô tả**: File vẫn giữ import `org.springframework.web.server.ResponseStatusException` và `org.springframework.http.HttpStatus` dù **không còn sử dụng** (đã chuyển sang `BusinessException`). Đây là dead import.
- **MD liên quan**: `tasknew.md` mục 2.1 và `finished-2.md` mục 2.1 tuyên bố đã chuyển `ReviewService` sang dùng `BusinessException`. Code đã chuyển đúng, nhưng **import cũ chưa dọn**.

### Lỗi 8: `TeacherSnapshot` trong `finished.md` — Mô tả 8 trường, code có 14 trường

- **File MD**: [finished.md:142](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/A/Task_A/finished.md#L142)
- **Code**: [TeacherSnapshot.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/teacher/facade/dto/TeacherSnapshot.java)
- **Mô tả**: `finished.md` mục A-04 ghi `TeacherSnapshot` có 8 trường: `id, userId, status (String), isVerified, isVisible, fullName, avatarUrl, bioExcerpt`. Nhưng code thực tế có **14 trường** (thêm `yearsOfExperience`, `supportsOnline`, `supportsOffline`, `languages`, `locationAddress`, `introductionVideoUrl`).
- **Hệ quả**: Tài liệu `finished.md` **lỗi thời**, không phản ánh đúng trạng thái hiện tại của code.

### Lỗi 9: `PricingPackageService.java` — Vẫn inject `EntityManager` nhưng không sử dụng

- **File**: [PricingPackageService.java:38](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/catalog/service/PricingPackageService.java#L38)
- **Mô tả**: `EntityManager` vẫn được inject (dòng 16: `import jakarta.persistence.EntityManager`, dòng 38: `private final EntityManager entityManager`) nhưng **không được sử dụng** ở bất kỳ method nào. 
- **MD liên quan**: `finished.md` mục A-10 tuyên bố "Xóa, dùng UUID trực tiếp" cho `EntityManager.getReference()`. Code đã xóa `getReference()` nhưng **chưa xóa `EntityManager` inject**.

### Lỗi 10: `Assignment.java` — Import thừa (unused imports)

- **File**: [Assignment.java:10-12](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/learning/domain/Assignment.java#L10-L12)
- **Mô tả**: Import `jakarta.persistence.FetchType`, `jakarta.persistence.JoinColumn`, `jakarta.persistence.ManyToOne` nhưng **không sử dụng** (đã chuyển sang UUID fields). Tương tự cho `Submission.java` (có import `FetchType`, `JoinColumn`, `ManyToOne` — nhưng Submission vẫn dùng `@ManyToOne Assignment` nên chỉ `Assignment.java` là có import thừa thực sự cho `FetchType`, `JoinColumn`, `ManyToOne` KHÔNG dùng trên field nào... À thực tế `Submission` vẫn có `@ManyToOne(fetch = FetchType.LAZY)` nên import hợp lệ. Nhưng **Assignment.java** đã chuyển sang UUID hoàn toàn, nên 3 import trên là **dead imports**).

### Lỗi 11: `PublicTeacherController.java` — Import trùng lặp

- **File**: [PublicTeacherController.java:30](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/catalog/controller/PublicTeacherController.java#L30)
- **Mô tả**: `import java.util.UUID;` xuất hiện **2 lần** (dòng 29 và dòng 30). Đây là lỗi import trùng lặp.

### Lỗi 12: `Submission.java` — Tài liệu ghi đã chuyển entity reference → UUID nhưng vẫn giữ `@ManyToOne Assignment`

- **File**: [Submission.java:36-38](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/learning/domain/Submission.java#L36-L38)
- **MD liên quan**: `finished.md` mục A-06 tuyên bố "Refactor `Submission` entity: thay entity reference → UUID". Nhưng code thực tế vẫn có:
  ```java
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assignment_id", nullable = false)
  private Assignment assignment;
  ```
  Đây là `@ManyToOne` **intra-module** (cùng module `learning`) nên có thể chấp nhận được, nhưng **tài liệu ghi sai** — nó nói đã chuyển toàn bộ sang UUID mà thực tế không phải.

---

## 🟡 LỖI LOGIC / KHÔNG NHẤT QUÁN

### Lỗi 13: `TeacherMarketplaceService.java` — `getTeacherDetail` vẫn gọi facade theo vòng lặp (N+1 tiềm ẩn)

- **File**: [TeacherMarketplaceService.java:46-50](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/catalog/service/TeacherMarketplaceService.java#L46-L50)
- **Mô tả**: Method `getTeacherDetail` vẫn lặp qua danh sách subject IDs rồi gọi `subjectFacade.getSubject(sid)` **từng cái một** (N+1 pattern). Mặc dù `searchTeachers` đã được fix (dùng `TeacherSearchRepository`), nhưng `getTeacherDetail` vẫn tồn tại pattern này.
- **MD liên quan**: `finishedn+1.md` mục 2 tuyên bố "Đã thay thế toàn bộ logic in-memory rườm rà" nhưng chỉ áp dụng cho `searchTeachers`, **không** cho `getTeacherDetail`.

### Lỗi 14: `ReviewRepository.java` — JPQL query dư thừa `isDeleted = false` khi Entity đã có `@Where`

- **File**: [ReviewRepository.java:17-24](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/repository/ReviewRepository.java#L17-L24)
- **Mô tả**: Các JPQL query (`findGlobalAverageRating`, `countVisibleReviewsByTeacherId`, `findAverageRatingByTeacherId`) đều có mệnh đề `AND r.isDeleted = false`. Nhưng entity `Review` đã có `@Where(clause = "is_deleted = false")` nên Hibernate **đã tự thêm** điều kiện này. Kết quả là điều kiện bị **duplicate** — không gây lỗi nhưng dư thừa và gây nhầm lẫn.

### Lỗi 15: `TeacherStatsService.java` — `getTeacherStats` dùng `RESOURCE_NOT_FOUND` thay vì error code cụ thể

- **File**: [TeacherStatsService.java:44](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/service/TeacherStatsService.java#L44)
- **Mô tả**: Dùng `ErrorCode.RESOURCE_NOT_FOUND` (generic) thay vì error code nghiệp vụ cụ thể.
- **MD liên quan**: `finishedn+1.md` mục 8 tuyên bố "Thay thế hoàn toàn `ResponseStatusException` [...] bằng `BusinessException(ErrorCode.RESOURCE_NOT_FOUND)`". Tuy đã chuyển sang `BusinessException` nhưng **error code vẫn generic**, chưa đổi sang code nghiệp vụ riêng (ví dụ `TEACHER_STATS_NOT_FOUND`).

### Lỗi 16: `TeacherAssignmentService.java` — Vẫn dùng `RESOURCE_NOT_FOUND` và `FORBIDDEN_RESOURCE` generic

- **File**: [TeacherAssignmentService.java:51-57](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/learning/service/TeacherAssignmentService.java#L51-L57)
- **Mô tả**: 
  - Dòng 51: `ErrorCode.RESOURCE_NOT_FOUND` khi student không tồn tại — nên là `STUDENT_NOT_FOUND`
  - Dòng 57: `ErrorCode.FORBIDDEN_RESOURCE` khi không có learning relationship — `ketluan.md` đã nêu vấn đề này (P1) nhưng `finishedn+1.md` mục 3 chỉ ghi "Các chuỗi cứng trong... đã được loại bỏ và map với ErrorCode đúng nghiệp vụ". Thực tế **vẫn generic**.

### Lỗi 17: `TeacherStatsService.java` — Vẫn import `HttpStatus` không sử dụng

- **File**: [TeacherStatsService.java:20](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/service/TeacherStatsService.java#L20)
- **Mô tả**: `import org.springframework.http.HttpStatus;` — không được sử dụng ở bất kỳ đâu trong file. Dead import.

### Lỗi 18: `TeacherStatsService.java` — `getGlobalRanking` gọi `teacherFacade.getTeacher()` trong vòng lặp (N+1)

- **File**: [TeacherStatsService.java:66-78](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/service/TeacherStatsService.java#L66-L78)
- **Mô tả**: Với mỗi `TeacherStats` trong page kết quả, code gọi `teacherFacade.getTeacher(stats.getTeacherId())` — tạo ra N queries bổ sung cho N items trong page. Đây là pattern N+1 nhưng **không** được nêu ra trong bất kỳ MD nào.

---

## 🔵 VẤN ĐỀ TÀI LIỆU (MD không chính xác)

### Lỗi 19: `finished.md` mục A-07 ghi `Conversation` đã chuyển entity reference → UUID

- **Dòng**: [finished.md:224](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/A/Task_A/finished.md#L224)
- **Mô tả**: MD ghi "Refactor `Chat`, `Message`, `Notification` entities: thay entity reference → UUID fields". Nhưng entity `Conversation` (tương đương `Chat`) vẫn **không** kế thừa `BaseEntity` (xem Lỗi 4).

### Lỗi 20: `PROGRESS_BE_A.md` — DoD đánh dấu sai

- **File**: [PROGRESS_BE_A.md:41-42](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/A/PROGRESS_BE_A.md#L41-L42)
- **Mô tả**: Dòng 41 ghi "Endpoint/DTO khớp API contract | ❌ Failed" — đây là **thành thật** nhưng mâu thuẫn trực tiếp với `finishedn+1.md` dòng 26 tuyên bố "Mã nguồn hiện tại hoàn toàn đáp ứng các quy chuẩn đề ra trong tài liệu hợp đồng". **Hai file nói ngược nhau**.

### Lỗi 21: `finishedn+1.md` mục 1 — Liệt kê 7 entity nhưng thực tế không đầy đủ

- **Dòng**: [finishedn+1.md:6](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/A/Task_A/finishedn+1.md#L6)
- **Mô tả**: Liệt kê "User, RefreshToken, Attachment, Assignment, Submission, Conversation, TeacherAvailability" đã có `@SQLDelete` + `@SQLRestriction`. Nhưng:
  - `Conversation` có `@SQLDelete` + `@SQLRestriction` ✅ nhưng **không kế thừa BaseEntity** ❌
  - Thiếu `TeacherProfile`, `TeacherDocument`, `TeacherSubject`, `Subject`, `SubjectProposal`, `PricingPackage` trong danh sách — thực tế các entity này cũng đã có `@SQLDelete` + `@Where`. Danh sách **không đầy đủ**.
  - Thiếu `Message`, `Notification` — vẫn **chưa** có soft-delete.

### Lỗi 22: `finishedn+1.md` mục 9 — Tuyên bố "khắc phục 100%" nhưng vẫn còn lỗi compile

- **Dòng**: [finishedn+1.md:46](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/A/Task_A/finishedn+1.md#L46)
- **Mô tả**: "hệ thống đã được tái cơ cấu để khắc phục 100% các lỗi nghiêm trọng" — nhưng `TeacherStats.java` vẫn thiếu import `@UpdateTimestamp` (Lỗi 1), nếu chưa xử lý thì không thể "100%".

### Lỗi 23: `finishedn+1.md` mục 8 tuyên bố "CORS xóa wildcard" — Code đã xử lý nhưng logic vẫn cho phép `*`

- **File code**: [SecurityConfig.java:76-77](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/common/config/SecurityConfig.java#L76-L77)
- **Mô tả**: MD ghi "Xóa quyền truy cập Origin `*` nguy hiểm". Nhưng code vẫn có logic:
  ```java
  if (origins.contains("*")) {
      configuration.setAllowedOriginPatterns(List.of("*"));
  }
  ```
  Nghĩa là nếu biến env `APP_CORS_ALLOWED_ORIGINS` chứa `*`, wildcard vẫn được chấp nhận. Chỉ là **default value** không còn `*` nữa, nhưng **code vẫn cho phép** cấu hình `*` từ env.

---

## 📊 TỔNG HỢP

| Mức độ | Số lỗi | Mô tả |
|--------|--------|-------|
| 🔴 Compile/Runtime | **2** | Thiếu import `@UpdateTimestamp`, conflict kiểu `ZonedDateTime` vs `Instant` |
| 🟠 Không đồng bộ MD↔Code | **10** | Entity chưa fix, import thừa, tài liệu ghi sai trạng thái |
| 🟡 Logic/Không nhất quán | **6** | N+1 tiềm ẩn, error code generic, query dư thừa |
| 🔵 Tài liệu sai | **5** | MD tuyên bố "done" nhưng code chưa khớp |
| **Tổng** | **23** | |

> [!CAUTION]
> **Lỗi 1** (`TeacherStats.java` thiếu import `@UpdateTimestamp`) là lỗi compile — nếu chưa fix thì project **không thể build** được. Cần kiểm tra lại xem `mvn clean verify` có thực sự pass hay không.

> [!WARNING]
> `Message.java` và `Notification.java` hoàn toàn không có soft-delete filter, không kế thừa `BaseEntity` — đây là blind spot lớn mà không có file MD nào đề cập đến.

---
---

# 📋 Báo cáo Review Phần 2: Code vs Architecture / Guidelines / Planning

> Đối chiếu code thực tế (phạm vi thành viên A — Identity & Experience) với:
> - [API_CONTRACT.md](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/API_CONTRACT.md)
> - [ERROR_CODES.md](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/ERROR_CODES.md)
> - [ERD.md](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/ERD.md)
> - [CODING_CONVENTION.md](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/guidelines/CODING_CONVENTION.md)
> - [PLANBE.md](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/planning/PLANBE.md)

---

## 🔴 LỖI NGHIÊM TRỌNG (Vi phạm API Contract / Architecture)

### Lỗi 24: `TeacherSearchParams` — Thiếu field `educationLevel` so với API_CONTRACT

- **Contract**: [API_CONTRACT.md:240-246](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/API_CONTRACT.md#L240-L246) ghi `TeacherSearchParams` chỉ có: `keyword, subjectId, minPrice, maxPrice, minRating, deliveryMode, dayOfWeek, startTime, endTime, page, size, sort`.
- **Code**: [TeacherSearchParams.java:21](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/catalog/dto/TeacherSearchParams.java#L21) có thêm field `educationLevel` — **field này không tồn tại trong API Contract**.
- **Hệ quả**: FE không biết field này tồn tại → API behavior không match contract.

### Lỗi 25: `AuthController` — Endpoint `/api/auth/oauth2/exchange` không có trong API Contract

- **Contract**: [API_CONTRACT.md:164-173](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/API_CONTRACT.md#L164-L173) chỉ liệt kê endpoint `POST /api/auth/oauth2/complete-registration`.
- **Code**: [AuthController.java:82-87](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/auth/controller/AuthController.java#L82-L87) có thêm endpoint `POST /api/auth/oauth2/exchange` — **không có trong contract**.
- **Hệ quả**: Endpoint không document, FE không biết.

### Lỗi 26: `Review.java` — Không kế thừa `BaseEntity` đúng cách

- **Code**: [Review.java:31](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/domain/Review.java#L31) `extends BaseEntity` ✅, nhưng vẫn import `CreationTimestamp`, `UpdateTimestamp`, `ZonedDateTime` mà **không sử dụng** (vì `BaseEntity` đã quản lý `createdAt`/`updatedAt` qua `@PrePersist`/`@PreUpdate`). Dead imports: dòng 13 (`CreationTimestamp`), 15 (`UpdateTimestamp`), 18 (`ZonedDateTime`), 5 (`GeneratedValue`).

### Lỗi 27: `TeacherStats.java` — ERD ghi `teacher_id` là PK nhưng code dùng `id` từ `BaseEntity`

- **ERD**: [ERD.md:233-244](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/ERD.md#L233-L244) ghi `TEACHER_STATS { uuid teacher_id PK,FK ... }` — `teacher_id` là Primary Key.
- **Code**: [TeacherStats.java:29](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/domain/TeacherStats.java#L29) kế thừa `BaseEntity` → PK là `id` (UUID sinh riêng), `teacher_id` chỉ là unique column.
- **Hệ quả**: Schema trong migration (do B quản lý) sẽ quyết định cái nào đúng, nhưng **code entity không match ERD**.

### Lỗi 28: `NotificationController` — Endpoint path không khớp API Contract

- **Contract**: [API_CONTRACT.md:684-686](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/API_CONTRACT.md#L684-L686) ghi:
  - `GET /api/notifications` (Authenticated)
  - `PATCH /api/notifications/{id}/read` (Owner)
  - `POST /api/notifications/read-all` (Authenticated)
- **Thêm**: [API_CONTRACT.md:481-482](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/API_CONTRACT.md#L481-L482) ghi **Student-specific**:
  - `GET /api/student/notifications`
  - `PATCH /api/student/notifications/{id}/read`
- **Code**: [NotificationController.java:18](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/communication/controller/NotificationController.java#L18) dùng `@RequestMapping({"/api/notifications", "/api/student/notifications"})` — mapping 2 path vào cùng controller. `PATCH /api/student/notifications/{id}/read` sẽ **không hoạt động** vì chỉ `@PatchMapping("/{id}/read")` match với base path `/api/notifications` hoặc `/api/student/notifications`, nhưng role check (`STUDENT`) **không tồn tại** — bất kỳ user authenticated nào cũng gọi được cả 2 path.

### Lỗi 29: `ConversationController` — Thiếu kiểm tra Role (Student/Teacher)

- **Contract**: [API_CONTRACT.md:681](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/API_CONTRACT.md#L681) ghi `GET /api/conversations` yêu cầu quyền `Student/Teacher`.
- **Code**: [ConversationController.java:20-22](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/communication/controller/ConversationController.java#L20-L22) path `/api/conversations` — không thuộc `/api/teacher/**` hoặc `/api/student/**` nên SecurityConfig sẽ match `.anyRequest().authenticated()`. Admin cũng có thể gọi endpoint này.
- **Hệ quả**: Admin có thể gọi endpoint conversation mà contract chỉ cho Student/Teacher.

---

## 🟠 LỖI KHÔNG ĐỒNG BỘ VỚI CODING_CONVENTION

### Lỗi 30: Wildcard import vi phạm CODING_CONVENTION mục 6

- **Convention**: [CODING_CONVENTION.md:364](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/guidelines/CODING_CONVENTION.md#L364) — "Không wildcard import."
- **Code vi phạm**:
  - [TeacherMarketplaceService.java:18](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/catalog/service/TeacherMarketplaceService.java#L18) — `import java.util.*;`
  - [TeacherSearchRepository.java:14](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/catalog/repository/TeacherSearchRepository.java#L14) — `import java.util.*;`

### Lỗi 31: Response DTO dùng `@Data` class thay vì immutable record — vi phạm CODING_CONVENTION mục 3.5

- **Convention**: [CODING_CONVENTION.md:154-155](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/guidelines/CODING_CONVENTION.md#L154-L155) — "Request/response là class/**record** riêng", "Ưu tiên immutable DTO/value object" (mục 6 dòng 366).
- **Code vi phạm**: Nhiều DTO dùng `@Data` + `@NoArgsConstructor` + `@AllArgsConstructor` thay vì `record`:
  - [SubmissionDetail.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/learning/dto/response/SubmissionDetail.java) — mutable class
  - [AssignmentDetail.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/learning/dto/response/AssignmentDetail.java) — mutable class
  - [ReviewView.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/dto/response/ReviewView.java) — mutable class
  - [TeacherStatsView.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/dto/response/TeacherStatsView.java) — mutable class
  - [TeacherRankingItem.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/dto/response/TeacherRankingItem.java) — mutable class
  - [NotificationView.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/communication/dto/notification/NotificationView.java) — mutable class
  - [ConversationView.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/communication/dto/chat/ConversationView.java) — mutable class
  - [MessageView.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/communication/dto/chat/MessageView.java) — mutable class
  - [ContentBlock.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/learning/dto/response/ContentBlock.java) — mutable class
  - [AttachmentView.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/common/dto/response/AttachmentView.java) — mutable class
- **Lưu ý**: `SubjectDto`, `TeacherCard`, `TeacherPublicDetail`, `PricingPackageView`, `UpsertPricingPackageRequest`, `TeacherSearchParams` đều dùng `record` — **không nhất quán** trong cùng 1 developer.

### Lỗi 32: Kiểu thời gian không nhất quán — vi phạm CODING_CONVENTION mục 1

- **Convention**: [CODING_CONVENTION.md:11](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/guidelines/CODING_CONVENTION.md#L11) — "Thời gian lưu UTC", [ERD.md:6](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/ERD.md#L6) — "Thời gian lưu: UTC (`timestamptz`/`Instant`)".
- **Code vi phạm**:
  - [TeacherStats.java:67](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/domain/TeacherStats.java#L67) — `calculatedAt` kiểu `ZonedDateTime` thay vì `Instant`
  - [Review.java:18](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/domain/Review.java#L18) — import `ZonedDateTime` (dù không dùng, nhưng cho thấy có thể từng dùng)
  - [Conversation.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/communication/domain/Conversation.java) và [Message.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/communication/domain/Message.java) — dùng `Instant` ✅ nhưng **không kế thừa BaseEntity** ❌

---

## 🟡 LỖI SO VỚI ERD / ERROR_CODES

### Lỗi 33: `Review` entity — Thiếu `@Version` theo ERD

- **ERD**: [ERD.md:555](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/ERD.md#L555) — "Mọi cột `version` dùng JPA `@Version`". Tuy ERD không ghi `version` cho `REVIEWS`, nhưng [CODING_CONVENTION.md:149](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/guidelines/CODING_CONVENTION.md#L149) ghi "Entity concurrent dùng `@Version`".
- **Code**: Không có `@Version` trên `Review.java` — nhưng nếu Review không concurrent-update thì có thể chấp nhận. Tuy nhiên: **PricingPackage** là entity duy nhất thuộc A có `@Version` (dòng 56). Không có entity nào khác của A có `@Version`.

### Lỗi 34: ERD ghi `REVIEWS.is_visible` nhưng code dùng `Boolean` wrapper thay vì `boolean` primitive

- **ERD**: [ERD.md:230](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/ERD.md#L230) — `boolean is_visible`
- **Code**: [Review.java:50](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/domain/Review.java#L50) — `private Boolean isVisible = true;` (wrapper `Boolean` thay vì `boolean`). Rủi ro NPE khi giá trị null.

### Lỗi 35: `PublicReviewController.java` — Toàn bộ class rỗng, code bị comment out

- **File**: [PublicReviewController.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/catalog/controller/PublicReviewController.java)
- **Mô tả**: Endpoint `GET /api/public/teachers/{id}/reviews` đã bị comment out ("TODO: Implement in M7"). Tuy nhiên, endpoint này **đã được implement** ở [ReviewController.java:39-45](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/ranking/controller/ReviewController.java#L39-L45). `PublicReviewController` là file thừa, gây nhầm lẫn.

### Lỗi 36: `TestEventPublisherController.java` — Controller test/debug không nên commit vào production

- **File**: [TestEventPublisherController.java](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/communication/controller/TestEventPublisherController.java)
- **Mô tả**: Controller test dùng để test event publishing. Không thuộc API Contract, không có trong planning. Nếu lộ ra production sẽ là lỗ hổng bảo mật — cho phép bất kỳ ai authenticated gửi notification/event tùy ý.

### Lỗi 37: `ConversationController` — Import entity `Conversation` trực tiếp trong Controller

- **Convention**: [CODING_CONVENTION.md:95](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/guidelines/CODING_CONVENTION.md#L95) — "Controller: parse request, annotation bảo mật, gọi service, chọn HTTP status. Không chứa business rule hay truy vấn repository." và [CODING_CONVENTION.md:16](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/guidelines/CODING_CONVENTION.md#L16) — "Không trả JPA Entity trực tiếp qua API."
- **Code**: [ConversationController.java:8](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/communication/controller/ConversationController.java#L8) — `import com.edtech.platform.communication.domain.Conversation;` — import entity trong controller (dù không dùng trong response, nhưng vi phạm nguyên tắc layer isolation).

### Lỗi 38: `ApiResponse.created()` — Không trả HTTP 201 thực sự

- **Contract**: [API_CONTRACT.md:118](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/architecture/API_CONTRACT.md#L118) — "201 Created: Tạo resource thành công".
- **Convention**: [CODING_CONVENTION.md:193](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/guidelines/CODING_CONVENTION.md#L193) — "Controller trả `ResponseEntity<ApiResponse<...>>` khi cần status/header"
- **Code**: [ApiResponse.java:20-26](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/common/response/ApiResponse.java#L20-L26) — `ApiResponse.created()` chỉ set `success=true` và message, nhưng **không trả HTTP 201** trừ khi controller có `@ResponseStatus(HttpStatus.CREATED)`.
- **Một số controller dùng `@ResponseStatus(CREATED)`**: `AuthController.register()` ✅, `TeacherAssignmentController.createAssignment()` ✅, `ReviewController.createReview()` ✅, `StudentAssignmentController.createSubmission()` ✅.
- **Nhưng `PricingPackageService`** được gọi từ `TeacherPricingPackageController` — cần kiểm tra xem có `@ResponseStatus(CREATED)` không.

---

## 🔵 LỖI SO VỚI PLANBE

### Lỗi 39: `catalog` module truy cập trực tiếp `teacher` module service — Vi phạm PLANBE mục "chống giẫm chân"

- **Plan**: [PLANBE.md:35](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/planning/PLANBE.md#L35) — "Module chỉ giao tiếp qua public facade, DTO hoặc domain event; không gọi repository của module khác."
- **Code**: [PublicTeacherController.java:13](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/backend/src/main/java/com/edtech/platform/catalog/controller/PublicTeacherController.java#L13) — import `com.edtech.platform.teacher.service.TeacherAvailabilityService` (truy cập **service** của module `teacher` trực tiếp thay vì qua **facade**).
- **Hệ quả**: Vi phạm boundary modular monolith. `catalog` nên gọi `TeacherFacade` thay vì `TeacherAvailabilityService` trực tiếp.

### Lỗi 40: Scope định nghĩa Module A vs thực tế — `catalog` không trong CODING_CONVENTION package list

- **Plan**: [PLANBE.md:14](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/planning/PLANBE.md#L14) — "A sở hữu `catalog`".
- **Convention**: [CODING_CONVENTION.md:44-76](file:///c:/Users/ADMIN/OneDrive/Desktop/edtech-tutor-marketplace/docs/guidelines/CODING_CONVENTION.md#L44-L76) — Package list không có `catalog`. Convention liệt kê: `auth, booking, finance, payment, teacher, subject, learning, communication, ranking, admin, scheduler, common`.
- **Hệ quả**: `catalog` package tồn tại trong code nhưng **không có trong CODING_CONVENTION**, gây nhầm lẫn cho developer mới.

---

## 📊 TỔNG HỢP PHẦN 2

| Mức độ | Số lỗi | Mô tả |
|--------|--------|-------|
| 🔴 Vi phạm API Contract / Architecture | **6** | Field/endpoint không khớp contract, role check thiếu, ERD mismatch |
| 🟠 Vi phạm Coding Convention | **3** | Wildcard import, mutable DTO, kiểu thời gian sai |
| 🟡 ERD / Error Codes / Code Quality | **6** | Entity dư thừa, controller test lộ, import entity sai layer |
| 🔵 PLANBE / Module Boundary | **2** | Cross-module service access, undocumented package |
| **Tổng Phần 2** | **17** | |

---

## 📊 TỔNG KẾT TOÀN BỘ (Phần 1 + Phần 2)

| Nhóm | Phần 1 | Phần 2 | Tổng |
|------|--------|--------|------|
| 🔴 Nghiêm trọng | 2 | 6 | **8** |
| 🟠 Không đồng bộ | 10 | 3 | **13** |
| 🟡 Logic/Quality | 6 | 6 | **12** |
| 🔵 Tài liệu/Plan | 5 | 2 | **7** |
| **Tổng cộng** | **23** | **17** | **40** |

> [!IMPORTANT]
> Trong 40 lỗi, có ít nhất **8 lỗi nghiêm trọng** ảnh hưởng trực tiếp đến: khả năng compile (thiếu import), API Contract mismatch (FE không thể gọi đúng), và bảo mật (role check thiếu, test controller lộ).
