# Diagnose Report — Backend của Thành viên A

> Trạng thái diagnose ban đầu: **Đã kiểm chứng bằng static analysis, Git history và `mvn test` ngày 2026-08-23.**
>
> Phạm vi: các module A sở hữu theo `docs/planning/PLANBE.md`. Báo cáo không sửa code và không thay thế Definition of Done (DoD) trong kế hoạch gốc.

## ✅ Trạng thái Giải quyết (Resolution Status)

> Cập nhật: **2026-08-24** — Toàn bộ issues được diagnose bên dưới **đã được giải quyết hoàn toàn** qua remediation A-01 → A-11.

| Issue | Section | Tiêu chí đóng | Trạng thái |
|---|---|---|---|
| Repository access xuyên module (12 class, 17 dep) | §2 | Không còn consumer inject repository ngoài module; architecture test ngăn dependency quay lại | ✅ Closed |
| SQL/JPQL vượt module boundary | §3 | Không dùng SQL/JPQL đọc bảng ngoài ownership; FixDbController đã xóa | ✅ Closed |
| JPA domain entity coupling xuyên module | §4 | Không facade trả JPA entity; toàn bộ cross-module ref chuyển sang UUID | ✅ Closed |
| Khoảng trống kiểm thử | §5 | 56 unit/integration tests, 0 failures, 0 errors; ArchUnit 3 rules, 0 violations | ✅ Closed |

**Bằng chứng:** `mvn test` ngày 2026-08-23/24 → Tests run: 56, Failures: 0, Errors: 0, Skipped: 8 (FlywayMigrationTest cần Docker). ArchUnit: 0 violations.
Xem chi tiết tại [`PROGRESS_BE_A.md`](./PROGRESS_BE_A.md) và [`Task_A/finished.md`](./Task_A/finished.md).

---

## 1. Căn cứ và khả năng truy nguyên

`PLANBE.md` yêu cầu module chỉ giao tiếp qua public facade, DTO hoặc domain event; không gọi repository module khác; có unit/integration test phù hợp rủi ro và toàn bộ backend test thành công.

Git history xác nhận phần lớn implementation được đưa vào bởi:

```text
e0ad25f — TLuon — feat: implement backend modules
```

Commit này thêm implementation thuộc `catalog`, `common`, `communication`, `learning`, `ranking`, `subject`, `teacher` và test Auth của A. Các thay đổi test infrastructure/migration sau đó thuộc commit/tác giả khác. Kết luận code được gắn với implementation của A; trạng thái test được đánh giá trên working tree hiện tại.

## 2. [CRITICAL] Repository access xuyên module

Static analysis xác nhận **12 consumer class với 17 dependency repository xuyên module**:

| Consumer module | Class | Repository ngoài module |
|---|---|---|
| `catalog` | `PricingPackageService` | `SubjectRepository`, `TeacherProfileRepository`, `TeacherSubjectRepository` |
| `common` | `AttachmentService` | `UserRepository` |
| `communication` | `ChatService` | `AttachmentRepository` |
| `communication` | `NotificationEventListener` | `UserRepository` |
| `learning` | `StudentAssignmentService` | `UserRepository`, `AttachmentRepository` |
| `learning` | `TeacherAssignmentService` | `UserRepository`, `AttachmentRepository`, `SubjectRepository`, `TeacherProfileRepository` |
| `ranking` | `ReviewService` | `UserRepository` |
| `ranking` | `TeacherStatsService` | `UserRepository`, `TeacherProfileRepository` |
| `subject` | `SubjectProposalService` | `TeacherProfileRepository` |
| `teacher` | `TeacherSubjectService` | `SubjectRepository` |

Consumer biết repository API và thường biết cả JPA entity của provider. Thay đổi method signature, entity mapping, fetch behavior hoặc query contract có thể gây lỗi compile hay runtime ở nhiều module. Một thay đổi schema tương thích không nhất thiết làm mọi module hỏng ngay, nhưng boundary hiện tại không bảo vệ consumer khỏi implementation nội bộ.

**Tiêu chí đóng:** không còn consumer inject/import repository ngoài module; mọi nhu cầu đi qua facade/query port, immutable DTO snapshot hoặc domain event; architecture test ngăn dependency quay lại.

## 3. [CRITICAL] SQL/JPQL vượt module boundary

Repository import không phải đường vòng duy nhất:

| Vị trí | Boundary bị vượt |
|---|---|
| `ReviewService` | Đọc bảng `bookings` của B để quyết định quyền review |
| `TeacherStatsService` | Đọc `bookings` và `platform_settings` của B |
| `StudentPackageChecker` | Đọc `student_packages` của B |
| `LearningRelationshipChecker` | Đọc quan hệ học qua bảng thuộc B |
| `TeacherMarketplaceService` | Join `users`, `teacher_profiles`, `teacher_subjects`, `subjects`, `teacher_availabilities`, `teacher_stats`, `pricing_packages` |
| `ReviewRepository` | JPQL join `TeacherProfile` và `User` ngoài ranking |
| `TeacherStatsRepository` | Native query join bảng teacher/auth/subject ngoài ranking |

`FixDbController` trong `common` còn thực thi SQL sửa schema qua HTTP, vượt ownership migration của B và phải được review/xóa khỏi runtime production.

**Tiêu chí đóng:** A không dùng SQL/JPQL để đọc bảng ngoài ownership; dữ liệu booking/enrollment/settings đi qua facade do B sở hữu; marketplace/ranking dùng contract hoặc read model được review; không còn runtime endpoint sửa schema.

## 4. [HIGH] JPA domain entity coupling xuyên module

Các ví dụ đã xác nhận:

- `PricingPackage` tham chiếu `TeacherProfile`, `Subject`.
- `Assignment` tham chiếu `User`, `TeacherProfile`, `Subject`; `Submission` tham chiếu `User`.
- `Attachment`, `TeacherProfile`, `TeacherDocument`, `SubjectProposal`, `TeacherSubject` tham chiếu entity module khác.
- Một số service/controller/DTO import enum hoặc entity ngoài module thay vì public contract.

Không sửa cơ học tất cả quan hệ trong một lần vì có thể tác động mapping/schema. Mỗi quan hệ phải được inventory và phân loại: giữ có quyết định, thay bằng ID, hoặc thay public contract; mọi thay đổi schema chuyển cho B.

**Tiêu chí đóng:** không facade nào trả JPA entity; không còn finding chưa có owner/quyết định; ngoại lệ boundary phải được review và ghi lại.

## 5. [CRITICAL] Khoảng trống kiểm thử

A có một test case nghiệp vụ: `AuthRegisterTest.registerStudentWithParentEmail_setsNotifyParentTrue()`.

Không tìm thấy unit/integration test nghiệp vụ tương ứng cho `catalog`, `common`, `communication`, `learning`, `ranking`, `subject` hoặc `teacher`.

Working tree có 8 test case Maven phát hiện: 1 context smoke test, 1 Auth register integration test của A và 6 Flyway migration tests. Kết quả `mvn test` ngày 2026-08-23:

```text
Tests run: 8, Failures: 0, Errors: 0, Skipped: 8
BUILD SUCCESS
```

Testcontainers không tìm thấy Docker nên toàn bộ test bị skip. Kết quả này chỉ chứng minh compile/test discovery, không chứng minh hành vi hoặc migration đúng.

**Tiêu chí đóng:** có unit test cho state/ownership/validation/idempotency/mapping; integration/contract test cho các facade và luồng chính; chạy Docker/Testcontainers với `0 failures`, `0 errors`, `0 skipped`; không đánh dấu Done nếu test bắt buộc bị skip.

## 6. [MEDIUM] Trạng thái tiến độ bị mô tả quá mức

Thuật ngữ chuẩn từ nay:

- `Implemented`: đã có implementation nền tảng.
- `Not DoD-compliant`: chưa thỏa Definition of Done.
- `Needs remediation`: phải xử lý boundary/test trước nghiệm thu.
- `Done`: chỉ dùng sau khi toàn bộ DoD và verification thực sự đạt.

## Kết luận

Code của A chưa đủ điều kiện nghiệm thu hoặc dùng làm baseline an toàn. Thứ tự bắt buộc: architecture guard → provider facade/contract test → consumer migration → test nghiệp vụ → full regression. Xem `REMEDIATION_PLAN_BE_A.md` và `TASKS_BE_A.md`.
