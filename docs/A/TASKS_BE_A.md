# Task Checklist — Remediation Backend Thành viên A

> Chỉ đánh dấu `[x]` khi acceptance criteria đạt và test bắt buộc đã chạy, không bị skip.

## A-01 — Architecture inventory và guard

- [x] Inventory repository/domain imports, JPQL/native SQL và `JdbcTemplate` table access.
- [x] Thêm architecture tests và fixture chứng minh từng rule fail khi vi phạm.
- [x] Mapping mọi finding sang owner và task ID; không blanket-ignore debt.

**Dependencies:** Không.  
**Test:** architecture rule positive/negative fixtures.  
**Acceptance:** bắt được repository, entity và SQL boundary violation.

## A-02 — Loại runtime schema mutation

- [x] Xóa/disable `FixDbController` khỏi runtime API.
- [x] Nếu schema thiếu, ghi yêu cầu để B tạo migration.

**Dependencies:** A-01.  
**Test:** endpoint không exposed; context load.  
**Acceptance:** A không còn đường HTTP/SQL sửa schema và không sửa Flyway.

## A-03 — Identity public facade

- [x] Cung cấp existence/status và public identity snapshot, không trả `User`.
- [x] Test found/not-found/inactive và migrate một consumer tracer bullet.

**Dependencies:** A-01.  
**Ảnh hưởng:** auth → common/communication/learning/ranking.  
**Acceptance:** consumer đã migrate không import auth repository/entity.

## A-04 — Teacher và Subject public facades

- [x] Teacher: resolve theo user, approval/public snapshot, subject assignment check.
- [x] Subject: resolve active/public subject snapshot.
- [x] Migrate `TeacherSubjectService`, `SubjectProposalService` và test contract.

**Dependencies:** A-03.  
**Test:** assign/unassign, create/list proposal, not-found/inactive/ownership.  
**Acceptance:** không còn repository/entity dependency chéo; REST behavior không đổi.

## A-05 — Attachment public facade

- [x] Upload dùng identity facade.
- [x] Attachment facade validate pending ownership/context, bind và lấy immutable view/URL.
- [x] Không trả `Attachment` hoặc expose repository.

**Dependencies:** A-03.  
**Test:** file validation, owner/context mismatch, bind một lần, not-found.  
**Acceptance:** attachment behavior được đóng gói trong `common`.

## A-06 — Learning tracer bullet

- [x] Migrate hai Assignment services khỏi User/Attachment/Subject/Teacher repository/entity.
- [x] Giữ transaction/invariant của Assignment/Submission trong learning.

**Dependencies:** A-03, A-04, A-05; B relationship facade nếu cần.  
**Test:** create, submit, idempotency/resubmit nếu có, grade, ownership, invalid inputs.  
**Acceptance:** architecture rule xanh và API/error behavior giữ nguyên.

## A-07 — Communication tracer bullet

- [x] Chat dùng attachment facade cho validate/bind/read URL.
- [x] Notification listener dùng identity snapshot hoặc event payload, không `UserRepository`.
- [x] Xác minh message idempotency và notification-after-commit.

**Dependencies:** A-03, A-05; authorization facade từ B.  
**Test:** text/attachment, duplicate client ID, owner/context mismatch, event notification.  
**Acceptance:** không còn common/auth repository/entity dependency.

## A-08 — Ranking tracer bullet

- [x] Review dùng booking eligibility của B và identity snapshot.
- [x] Stats dùng booking/settings của B và teacher snapshot.
- [x] Bỏ repository query join table/entity ngoài ownership.
- [x] Giữ Bayesian calculation, cache invalidation và event behavior.

**Dependencies:** A-03, A-04; booking/settings facade từ B.  
**Test:** eligibility/duplicate, public mapping, Bayesian cases, stats, cache invalidation.  
**Acceptance:** ranking không đọc bảng/entity/repository ngoài boundary.

## A-09 — Catalog tracer bullet

- [x] Pricing dùng teacher/subject facade.
- [x] Thay checker SQL bằng enrollment/relationship facade của B.
- [x] Marketplace dùng read model/facade composition đã review.
- [x] Giữ filter, pagination, pricing immutability và cache.

**Dependencies:** A-04; enrollment facade từ B.  
**Test:** create/update/status, approval, subject assignment, immutability, filters/detail.  
**Acceptance:** catalog không access repository/SQL/entity ngoài boundary.

## A-10 — Domain coupling cleanup

- [x] Phân loại mọi JPA entity reference: giữ có quyết định, thay ID hoặc public contract.
- [x] Làm thay đổi không cần schema trước; giao B mọi migration cần thiết.

**Dependencies:** A-03 đến A-09.  
**Test:** persistence mapping, serialization, facade contract và aggregate regression.  
**Acceptance:** inventory không còn finding thiếu quyết định/owner.

## A-11 — Full regression và nghiệm thu

- [x] Chạy architecture/unit/contract/integration test trên Docker/Testcontainers.
- [x] Đối chiếu `API_CONTRACT.md`, `ERROR_CODES.md`; chạy migration từ DB rỗng.
- [x] Cập nhật `PROGRESS_BE_A.md` bằng bằng chứng và tạo walkthrough sau cùng.

**Dependencies:** A-01 đến A-10.  
**Acceptance:** `mvn test`: `0 failures`, `0 errors`, `0 skipped`; không boundary finding mở; khi đó mới chuyển `Done`.

## Mẫu progress log bắt buộc

```markdown
## [YYYY-MM-DD HH:mm] [Task ID] — <Tên task>
- Mục tiêu:
- Căn cứ tài liệu:
- File tạo/sửa/xóa:
- Public contract hoặc quyết định kỹ thuật:
- Test đã chạy và failures/errors/skipped:
- Trạng thái: Done / Needs review / Blocked
- Việc còn thiếu hoặc dependency phía B:
```

Không ghi `Done` nếu chỉ compile, Testcontainers không chạy hoặc acceptance criteria còn thiếu.
