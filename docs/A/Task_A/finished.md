# Finished — Backend Thành viên A (A-01 → A-11)

> Tài liệu này ghi lại **chi tiết và đầy đủ** tất cả những gì đã thực hiện, phát hiện, sửa chữa và xác minh  
> trong toàn bộ quá trình remediation backend từ A-01 đến A-11.  
> Cập nhật lần cuối: 2026-08-23

---

## Kết quả cuối cùng (Evidence)

| Chỉ số | Giá trị |
|---|---|
| `mvn clean verify` lần cuối | **BUILD SUCCESS** |
| Tests run | **56** |
| Failures | **0** |
| Errors | **0** |
| Skipped | **8** *(FlywayMigrationTest — cần Docker)* |
| ArchUnit violations | **0** |
| Cross-module entity coupling còn lại | **0** |
| EntityManager.getReference() trong production | **0** |
| Entries còn active trong archunit_ignore_patterns.txt | **4** *(JdbcTemplate — hợp lệ, có owner)* |

---

## Diagnose Pass — Kết quả kiểm tra conflict A-01 → A-11

### Scan cross-module User entity
- `import com.edtech.platform.auth.domain.User` chỉ còn trong:
  - `auth.service.AuthService` ✅ (trong module auth — hợp lệ)
  - `auth.security.OAuth2AuthenticationSuccessHandler` ✅ (trong module auth — hợp lệ)
  - `auth.repository.UserRepository` ✅ (trong module auth — hợp lệ)
- **Không còn** User import nào trong module ngoài auth.

### Scan cross-module UserRepository
- Chỉ còn trong `auth.*` — **sạch**.

### Scan cross-module TeacherProfile entity
- Chỉ còn trong `teacher.*` (service, repository, facade impl) — **sạch**.

### Scan cross-module Subject entity
- Chỉ còn trong `subject.*` — **sạch**.

### Scan cross-module TeacherProfileRepository
- Chỉ còn trong `teacher.*` — **sạch**.

### Scan cross-module TeacherStatsRepository
- Còn trong `ranking.service.TeacherStatsService` ✅ (cùng module ranking)
- Còn trong `ranking.facade.impl.TeacherStatsFacadeImpl` ✅ (cùng module ranking)
- **Không còn** trong module ngoài ranking.

### Scan EntityManager.getReference()
- **0 kết quả** — đã xóa hoàn toàn.

### Scan @ManyToOne trong catalog
- **0 kết quả** — `PricingPackage` đã chuyển sang UUID hoàn toàn.

### Scan @ManyToOne trong teacher
- `TeacherSubject.teacher` → `@ManyToOne TeacherProfile` ✅ (internal within teacher module — hợp lệ)
- `TeacherDocument.teacher` → `@ManyToOne TeacherProfile` ✅ (internal — hợp lệ)
- `TeacherAvailability.teacher` → `@ManyToOne TeacherProfile` ✅ (internal — hợp lệ)
- **Không có** @ManyToOne cross-module nào.

### Kết luận diagnose
> **Không tìm thấy conflict hay lỗi kiến trúc nào.** Toàn bộ cross-module coupling đã được loại bỏ. Các `@ManyToOne` còn lại là intra-module (trong cùng package `teacher`) — hợp lệ theo quy tắc ArchUnit. `archunit_ignore_patterns.txt` chỉ còn 4 entries JdbcTemplate có owner rõ ràng (B hoặc A-09/A-08).

---

## Chi tiết từng Task

---

### A-01 — Architecture Inventory & Guard

**Mục tiêu:** Tạo ArchUnit test làm "fence" để bắt mọi cross-module violation từ đây về sau.

**Việc đã làm:**
- Tạo `ArchitectureTest.java` với 3 rules:
  1. `no_cross_module_repository_access` — cấm module X dùng Repository của module Y
  2. `no_cross_module_entity_access` — cấm module X dùng Domain/Entity của module Y
  3. `no_direct_jdbc_template_access` — cấm dùng JdbcTemplate ngoài Infrastructure layer
- Tạo `ArchitectureFixtureTest.java` chứng minh fixture bắt được lỗi khi vi phạm
- Tạo `archunit_ignore_patterns.txt` để inventory **explicit** tất cả debt hiện tại (không blanket-ignore)
- Mapping toàn bộ findings sang task ID (A-03, A-04, A-05, A-06, A-07, A-08, A-09, A-10, B)

**File tạo/sửa:**
- `src/test/java/.../architecture/ArchitectureTest.java` (NEW)
- `src/test/java/.../architecture/ArchitectureFixtureTest.java` (NEW)
- `src/test/resources/archunit_ignore_patterns.txt` (NEW)

**Kết quả:** ArchUnit guard hoạt động — bắt được vi phạm khi không có trong ignore list.

---

### A-02 — Loại Runtime Schema Mutation

**Mục tiêu:** Xóa `FixDbController` — controller cho phép sửa schema trực tiếp qua HTTP API.

**Việc đã làm:**
- Xóa hoàn toàn `FixDbController.java` khỏi codebase
- Tạo `RemovedEndpointTest.java` — integration test xác minh `/api/fix-db/**` trả 404
- Ghi yêu cầu migration cho Thành viên B: V18 DROP NOT NULL các cột `deleted` của `users`, `teacher_stats`, `reviews`

**File tạo/sửa:**
- `src/main/java/.../FixDbController.java` (DELETED)
- `src/test/java/.../common/controller/RemovedEndpointTest.java` (NEW)

**Kết quả:** Endpoint không còn exposed. Context load thành công.

---

### A-03 — Identity Public Facade

**Mục tiêu:** Đóng gói bảng `users` sau `IdentityFacade` — không module nào được phép import `User` hay `UserRepository`.

**Việc đã làm:**
- Tạo `IdentityFacade` interface với methods: `existsById`, `isActive`, `getIdentity`
- Tạo `IdentitySnapshot` record (immutable DTO): `id`, `email`, `fullName`, `role`, `status`, `avatarUrl`, `notifyParent`, `parentEmail`
- Tạo `IdentityFacadeImpl` implement từ `UserRepository`
- Migrate tracer bullet `AttachmentService` sang dùng `IdentityFacade` thay vì `UserRepository`
- Migrate `ChatService` sang dùng `IdentityFacade`
- Tạo `IdentityFacadeImplTest` (3 unit tests: existsById, isActive, getIdentity)
- Xóa sạch các violation A-03 khỏi `archunit_ignore_patterns.txt`

**File tạo/sửa:**
- `auth/facade/IdentityFacade.java` (NEW)
- `auth/facade/dto/IdentitySnapshot.java` (NEW)
- `auth/facade/impl/IdentityFacadeImpl.java` (NEW)
- `common/service/AttachmentService.java` (MODIFIED)
- `communication/service/ChatService.java` (MODIFIED)
- `auth/facade/impl/IdentityFacadeImplTest.java` (NEW)

**Kết quả:** Consumer đã migrate không import auth repository/entity. ArchUnit xanh cho A-03.

---

### A-04 — Teacher & Subject Public Facades

**Mục tiêu:** Đóng gói `TeacherProfile` và `Subject` sau Facades — không module ngoài được import entity/repository của teacher hoặc subject.

**Việc đã làm:**
- Tạo `TeacherFacade` interface: `getTeacher`, `getTeacherByUserId`, `hasAssignedSubject`
- Tạo `TeacherSnapshot` record: `id`, `userId`, `status` (String), `isVerified`, `isVisible`, `fullName`, `avatarUrl`, `bioExcerpt`
- Tạo `TeacherFacadeImpl` — dùng `IdentityFacade` để lấy fullName/avatarUrl
- Tạo `SubjectFacade` interface: `getSubject`, `isSubjectActive`
- Tạo `SubjectSnapshot` record: `id`, `code`, `name`, `educationLevel` (String), `isActive`
- Tạo `SubjectFacadeImpl` — convert `EducationLevel` enum → String khi build snapshot
- Migrate `SubjectProposalService` dùng `TeacherFacade`
- Migrate `TeacherSubjectService` dùng `SubjectFacade`
- Tạo `TeacherFacadeImplTest` (6 unit tests)
- Tạo `SubjectFacadeImplTest` (5 unit tests)
- Tạo `TeacherSubjectServiceTest` (2 unit tests)
- Tạo `SubjectProposalServiceTest` (1 unit test)
- Xóa sạch violations A-04 khỏi ignore list

**File tạo/sửa:**
- `teacher/facade/TeacherFacade.java` (NEW)
- `teacher/facade/dto/TeacherSnapshot.java` (NEW)
- `teacher/facade/impl/TeacherFacadeImpl.java` (NEW)
- `subject/facade/SubjectFacade.java` (NEW)
- `subject/facade/dto/SubjectSnapshot.java` (NEW — educationLevel: String)
- `subject/facade/impl/SubjectFacadeImpl.java` (NEW)
- `subject/service/SubjectProposalService.java` (MODIFIED)
- `teacher/service/TeacherSubjectService.java` (MODIFIED)

**Kết quả:** Không còn cross-module entity/repository dependency cho teacher/subject. 14 unit tests.

---

### A-05 — Attachment Public Facade

**Mục tiêu:** Đóng gói `Attachment` business logic sau `AttachmentFacade` — validate ownership, bind và lấy view/URL mà không trả entity.

**Việc đã làm:**
- Tạo `AttachmentFacade` interface: `validateAndBind(UUID attachmentId, UUID ownerId, String expectedType, UUID targetId)`, `getAttachmentView`, `getPublicUrl`
- Tạo `AttachmentFacadeImpl` — enforce: pending ownership, type match, single-bind idempotency
- Cập nhật `AttachmentService.upload` dùng `IdentityFacade` thay vì query `UserRepository`
- Cập nhật `AttachmentFacade.validateAndBind` nhận `String expectedType` (không phải enum) — tránh rò rỉ `AttachableType` ra ngoài module Common
- Tạo `AttachmentFacadeImplTest` (7 unit tests: pending ownership, type mismatch, bind success, duplicate bind, not found)
- Tạo `AttachmentServiceTest` (2 unit tests)
- Xóa sạch violations A-05 khỏi ignore list

**File tạo/sửa:**
- `common/facade/AttachmentFacade.java` (NEW)
- `common/facade/impl/AttachmentFacadeImpl.java` (NEW)
- `common/service/AttachmentService.java` (MODIFIED)

**Kết quả:** Attachment behavior đóng gói trong Common module. 9 unit tests.

---

### A-06 — Learning Tracer Bullet

**Mục tiêu:** Migrate `StudentAssignmentService` và `TeacherAssignmentService` khỏi User/Attachment/Subject/Teacher repository/entity.

**Việc đã làm:**
- Refactor `Assignment` entity: thay `@ManyToOne User teacher` → `@Column UUID teacherId`, `@ManyToOne User student` → `@Column UUID studentId`, `@ManyToOne Subject subject` → `@Column UUID subjectId`
- Refactor `Submission` entity: thay entity reference → UUID
- Refactor `AssignmentStatistics` entity: thay entity reference → UUID
- Tạo `AssignmentFacade`, `SubmissionFacade`, `AssignmentStatisticsFacade`
- Cập nhật `TeacherAssignmentService` dùng `TeacherFacade`, `SubjectFacade`, `IdentityFacade`
- Cập nhật `StudentAssignmentService` dùng `IdentityFacade`, `AttachmentFacade`
- Cập nhật idempotency trong `AttachmentFacade.validateAndBind` để hỗ trợ resubmit
- Xóa sạch violations A-06 khỏi ignore list

**File tạo/sửa:**
- `learning/domain/Assignment.java` (MODIFIED — UUID fields)
- `learning/domain/Submission.java` (MODIFIED — UUID fields)
- `learning/domain/AssignmentStatistics.java` (MODIFIED — UUID fields)
- `learning/facade/AssignmentFacade.java` (NEW)
- `learning/facade/SubmissionFacade.java` (NEW)
- `learning/facade/AssignmentStatisticsFacade.java` (NEW)
- `learning/service/TeacherAssignmentService.java` (MODIFIED)
- `learning/service/StudentAssignmentService.java` (MODIFIED)

**Kết quả:** Learning module không còn cross-module dependency. ArchUnit xanh.

---

### A-07 — Communication Tracer Bullet

**Mục tiêu:** Migrate Chat/Message/Notification khỏi User/Attachment repository/entity.

**Việc đã làm:**
- Refactor `Chat`, `Message`, `Notification` entities: thay entity reference → UUID fields (`senderId`, `receiverId`, `participantId`, `chatId`)
- Tạo `ChatFacade`, `MessageFacade`, `NotificationFacade` interfaces + implementations
- Cập nhật `ChatService` dùng `AttachmentFacade` thay vì `AttachmentRepository`
- Cập nhật `NotificationEventListener` dùng `IdentityFacade` thay vì `UserRepository`
- Thêm `notifyParent` và `parentEmail` vào `IdentitySnapshot`
- Xóa sạch violations A-07 khỏi ignore list

**File tạo/sửa:**
- `communication/domain/Chat.java` (MODIFIED — UUID fields)
- `communication/domain/Message.java` (MODIFIED — UUID fields)
- `communication/domain/Notification.java` (MODIFIED — UUID fields)
- `communication/facade/ChatFacade.java` (NEW)
- `communication/facade/MessageFacade.java` (NEW)
- `communication/facade/NotificationFacade.java` (NEW)
- `communication/service/ChatService.java` (MODIFIED)
- `communication/service/NotificationEventListener.java` (MODIFIED)
- `auth/facade/dto/IdentitySnapshot.java` (MODIFIED — thêm notifyParent, parentEmail)

**Kết quả:** Communication module không còn cross-module dependency.

---

### A-08 — Ranking Tracer Bullet

**Mục tiêu:** Migrate `ReviewService`, `TeacherStatsService` khỏi User/TeacherProfile repository/entity.

**Việc đã làm:**
- Refactor `Review`, `TeacherStats` entities: thay entity reference → UUID fields (`teacherId`, `studentId`)
- Tạo `ReviewFacade`, `TeacherStatsFacade` interfaces
- Cập nhật `ReviewService` dùng `IdentityFacade`, `BookingEligibilityFacade`
- Cập nhật `TeacherStatsService` dùng `TeacherFacade`, `IdentityFacade`
- Xóa sạch violations A-08 khỏi ignore list

**File tạo/sửa:**
- `ranking/domain/Review.java` (MODIFIED — UUID fields)
- `ranking/domain/TeacherStats.java` (MODIFIED — UUID fields)
- `ranking/facade/ReviewFacade.java` (NEW)
- `ranking/service/ReviewService.java` (MODIFIED)
- `ranking/service/TeacherStatsService.java` (MODIFIED)

**Kết quả:** Ranking module không còn cross-module dependency.

---

### A-09 — Catalog Tracer Bullet

**Mục tiêu:** Migrate Catalog (PricingPackage, Marketplace) khỏi TeacherProfile/Subject entity, thay checker SQL bằng `EnrollmentFacade`.

**Việc đã làm:**
- Tạo `EnrollmentFacade` interface: `hasValidRelationship(teacherId, studentId)`, `hasStudentPackage(pricingPackageId)`
- Tạo `EnrollmentFacadeImpl` dùng `JdbcTemplate` (legitimate cross-module read, có owner B)
- Refactor `PricingPackageService` dùng `TeacherFacade`, `SubjectFacade`, `EnrollmentFacade`
- Xóa `StudentPackageChecker`, `LearningRelationshipChecker`
- Tạo `EnrollmentFacadeImplTest` (4 unit tests)
- Xóa sạch violations A-09 khỏi ignore list

**File tạo/sửa:**
- `enrollment/facade/EnrollmentFacade.java` (NEW)
- `enrollment/facade/impl/EnrollmentFacadeImpl.java` (NEW)
- `catalog/service/PricingPackageService.java` (MODIFIED)
- `catalog/StudentPackageChecker.java` (DELETED)
- `catalog/LearningRelationshipChecker.java` (DELETED)
- `enrollment/facade/impl/EnrollmentFacadeImplTest.java` (NEW)

**Kết quả:** Catalog module không còn cross-module dependency.

---

### A-10 — Domain Coupling Cleanup

**Mục tiêu:** Loại bỏ các JPA entity coupling còn sót lại sau A-03 đến A-09.

**Danh sách findings và fix:**

| Class | Coupling cũ | Fix áp dụng |
|---|---|---|
| `SubjectProposal` | `@ManyToOne User reviewedBy` | → `@Column UUID reviewedById` |
| `SubjectSnapshot` | `EducationLevel educationLevel` (enum) | → `String educationLevel` |
| `SubjectFacadeImpl` | Trả `EducationLevel` enum | Convert `.name()` khi map snapshot |
| `PricingPackage` | `@ManyToOne TeacherProfile teacher`, `@ManyToOne Subject subject` | → `UUID teacherId`, `UUID subjectId` |
| `PricingPackageService` | Dùng `EntityManager.getReference()` | Xóa, dùng UUID trực tiếp |
| `PublicTeacherController` | `pkg.getSubject().getId()` | → `pkg.getSubjectId()` |
| `TeacherSubjectProposalController` | Import `ProposalStatus` (cross-module) | Gọi String overload của service |
| `SubjectProposalService` | Chỉ có ProposalStatus overload | Thêm `getProposals(UUID, String, Pageable)` |
| `UserRegisteredEvent` | Expose `getRole()` trả `Role` enum | Thêm `getRoleName()` trả String |
| `TeacherRegistrationEventListener` | `event.getRole().name()` (import Role) | → `event.getRoleName()` |
| `TeacherStatsJob` | Inject `TeacherStatsRepository` trực tiếp | → Dùng `TeacherStatsFacade` |
| `TeacherStatsFacadeImpl` | — | NEW: implement `updateAllGlobalRanks()` |
| `TeacherStatsFacade` | — | NEW: interface trong ranking.facade |
| `archunit_ignore_patterns.txt` | Nhiều entries đã resolved | Xóa toàn bộ stale entries |

**Bug tìm được & fix:**
- `PricingPackageService.java` thiếu `@Slf4j` → thêm annotation + import
- `TeacherProfile.java` thiếu `import java.util.UUID` → thêm import
- `TeacherFacadeImpl.java` gọi `identityFacade.getUser()` (method không tồn tại) → sửa thành `getIdentity()`

**Kết quả:** Test compile + run pass. ArchUnit 0 violations sau A-10.

---

### A-11 — Full Regression & Nghiệm thu (TDD)

**Mục tiêu:** Chạy toàn bộ test suite, bổ sung coverage cho behavior rủi ro cao, xác nhận 0 violations.

**TDD vertical slices thêm mới:**

#### PricingPackageServiceTest (5 slices)
1. `createPackage_throwsTeacherNotApproved_whenStatusNotApproved` — teacher PENDING → `TEACHER_NOT_APPROVED`
2. `createPackage_throwsSubjectNotAssigned_whenSubjectNotAssigned` — subject không assign → `SUBJECT_NOT_ASSIGNED`
3. `createPackage_success_whenTeacherApprovedAndSubjectAssigned` — happy path, verify save()
4. `changeStatus_throwsPackageInvalidState_whenHasStudentsAndNotInactive` — immutability rule
5. `changeStatus_allowsInactive_whenHasStudents` — INACTIVE always allowed

#### TeacherStatsFacadeImplTest (3 slices)
1. `updateAllGlobalRanks_assignsRank1ToHighestBayesian` — sort order đúng
2. `updateAllGlobalRanks_handlesEmptyList` — không crash khi rỗng
3. `updateAllGlobalRanks_tieBreaksBySessionCount` — tie-break by completedSessionCount

#### ReviewServiceTest (3 slices)
1. `createReview_throwsConflict_whenBookingAlreadyReviewed` — 409 CONFLICT
2. `createReview_throwsUnprocessable_whenBookingNotEligible` — 422 UNPROCESSABLE
3. `createReview_savesReviewAndPublishesEvent_whenEligible` — save + publishEvent

**Bug fix phát hiện qua TDD (production code):**
- `TeacherStatsFacadeImpl.updateAllGlobalRanks()`: `teacherStatsRepository.findAll()` trả `List.of()` (immutable) → gọi `.sort()` bị `UnsupportedOperationException` → Fix: `new ArrayList<>(teacherStatsRepository.findAll())`

**File tạo:**
- `catalog/service/PricingPackageServiceTest.java` (NEW — 5 tests)
- `ranking/facade/impl/TeacherStatsFacadeImplTest.java` (NEW — 3 tests)
- `ranking/service/ReviewServiceTest.java` (NEW — 3 tests)

**Kết quả cuối:**
```
Tests run: 56, Failures: 0, Errors: 0, Skipped: 8
BUILD SUCCESS
```

---

## Diagnose Summary — Không có conflict hay bug còn lại

| Mục kiểm tra | Kết quả |
|---|---|
| Cross-module User import trong production | ✅ Chỉ trong `auth.*` — hợp lệ |
| Cross-module UserRepository import | ✅ Chỉ trong `auth.*` — hợp lệ |
| Cross-module TeacherProfile import | ✅ Chỉ trong `teacher.*` — hợp lệ |
| Cross-module Subject import | ✅ Chỉ trong `subject.*` — hợp lệ |
| Cross-module TeacherProfileRepository | ✅ Chỉ trong `teacher.*` — hợp lệ |
| EntityManager.getReference() | ✅ **0 occurrences** |
| @ManyToOne cross-module | ✅ **0 occurrences** |
| ArchUnit no_cross_module_repository_access | ✅ **PASS** |
| ArchUnit no_cross_module_entity_access | ✅ **PASS** |
| ArchUnit no_direct_jdbc_template_access | ✅ **PASS** (4 ignores có owner) |
| mvn clean verify | ✅ **BUILD SUCCESS** |

---

## Tồn đọng (không phải lỗi — có quyết định)

| Item | Lý do | Owner |
|---|---|---|
| 8 FlywayMigrationTest bị skip | Cần Docker/Testcontainers, không khả dụng trên máy hiện tại | Infra |
| `TeacherSubject.teacher` @ManyToOne | Intra-module (teacher → teacher) — hợp lệ theo design | A |
| `TeacherDocument.teacher` @ManyToOne | Intra-module — hợp lệ | A |
| `TeacherAvailability.teacher` @ManyToOne | Intra-module — hợp lệ | A |
| JdbcTemplate trong EnrollmentFacadeImpl | Legitimate cross-module read facade — owner B, ignore documented | B |
| JdbcTemplate trong BookingEligibilityFacadeImpl | Owner B | B |
| JdbcTemplate trong PlatformSettingsFacadeImpl | Owner B | B |
| JdbcTemplate trong TeacherMarketplaceService | Legitimate read model — documented | B |
