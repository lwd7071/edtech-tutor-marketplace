# Task Checklist — Remediation Backend Thành viên A

> Chỉ đánh dấu `[x]` khi acceptance criteria đạt và test bắt buộc đã chạy, không bị skip.

## A-01 — Architecture inventory và guard

- [ ] Inventory repository/domain imports, JPQL/native SQL và `JdbcTemplate` table access.
- [ ] Thêm architecture tests và fixture chứng minh từng rule fail khi vi phạm.
- [ ] Mapping mọi finding sang owner và task ID; không blanket-ignore debt.

**Dependencies:** Không.  
**Test:** architecture rule positive/negative fixtures.  
**Acceptance:** bắt được repository, entity và SQL boundary violation.

## A-02 — Loại runtime schema mutation

- [ ] Xóa/disable `FixDbController` khỏi runtime API.
- [ ] Nếu schema thiếu, ghi yêu cầu để B tạo migration.

**Dependencies:** A-01.  
**Test:** endpoint không exposed; context load.  
**Acceptance:** A không còn đường HTTP/SQL sửa schema và không sửa Flyway.

## A-03 — Identity public facade

- [ ] Cung cấp existence/status và public identity snapshot, không trả `User`.
- [ ] Test found/not-found/inactive và migrate một consumer tracer bullet.

**Dependencies:** A-01.  
**Ảnh hưởng:** auth → common/communication/learning/ranking.  
**Acceptance:** consumer đã migrate không import auth repository/entity.

## A-04 — Teacher và Subject public facades

- [ ] Teacher: resolve theo user, approval/public snapshot, subject assignment check.
- [ ] Subject: resolve active/public subject snapshot.
- [ ] Migrate `TeacherSubjectService`, `SubjectProposalService` và test contract.

**Dependencies:** A-03.  
**Test:** assign/unassign, create/list proposal, not-found/inactive/ownership.  
**Acceptance:** không còn repository/entity dependency chéo; REST behavior không đổi.

## A-05 — Attachment public facade

- [ ] Upload dùng identity facade.
- [ ] Attachment facade validate pending ownership/context, bind và lấy immutable view/URL.
- [ ] Không trả `Attachment` hoặc expose repository.

**Dependencies:** A-03.  
**Test:** file validation, owner/context mismatch, bind một lần, not-found.  
**Acceptance:** attachment behavior được đóng gói trong `common`.

## A-06 — Learning tracer bullet

- [ ] Migrate hai Assignment services khỏi User/Attachment/Subject/Teacher repository/entity.
- [ ] Giữ transaction/invariant của Assignment/Submission trong learning.

**Dependencies:** A-03, A-04, A-05; B relationship facade nếu cần.  
**Test:** create, submit, idempotency/resubmit nếu có, grade, ownership, invalid inputs.  
**Acceptance:** architecture rule xanh và API/error behavior giữ nguyên.

## A-07 — Communication tracer bullet

- [ ] Chat dùng attachment facade cho validate/bind/read URL.
- [ ] Notification listener dùng identity snapshot hoặc event payload, không `UserRepository`.
- [ ] Xác minh message idempotency và notification-after-commit.

**Dependencies:** A-03, A-05; authorization facade từ B.  
**Test:** text/attachment, duplicate client ID, owner/context mismatch, event notification.  
**Acceptance:** không còn common/auth repository/entity dependency.

## A-08 — Ranking tracer bullet

- [ ] Review dùng booking eligibility của B và identity snapshot.
- [ ] Stats dùng booking/settings của B và teacher snapshot.
- [ ] Bỏ repository query join table/entity ngoài ownership.
- [ ] Giữ Bayesian calculation, cache invalidation và event behavior.

**Dependencies:** A-03, A-04; booking/settings facade từ B.  
**Test:** eligibility/duplicate, public mapping, Bayesian cases, stats, cache invalidation.  
**Acceptance:** ranking không đọc bảng/entity/repository ngoài boundary.

## A-09 — Catalog tracer bullet

- [ ] Pricing dùng teacher/subject facade.
- [ ] Thay checker SQL bằng enrollment/relationship facade của B.
- [ ] Marketplace dùng read model/facade composition đã review.
- [ ] Giữ filter, pagination, pricing immutability và cache.

**Dependencies:** A-04; enrollment facade từ B.  
**Test:** create/update/status, approval, subject assignment, immutability, filters/detail.  
**Acceptance:** catalog không access repository/SQL/entity ngoài boundary.

## A-10 — Domain coupling cleanup

- [ ] Phân loại mọi JPA entity reference: giữ có quyết định, thay ID hoặc public contract.
- [ ] Làm thay đổi không cần schema trước; giao B mọi migration cần thiết.

**Dependencies:** A-03 đến A-09.  
**Test:** persistence mapping, serialization, facade contract và aggregate regression.  
**Acceptance:** inventory không còn finding thiếu quyết định/owner.

## A-11 — Full regression và nghiệm thu

- [ ] Chạy architecture/unit/contract/integration test trên Docker/Testcontainers.
- [ ] Đối chiếu `API_CONTRACT.md`, `ERROR_CODES.md`; chạy migration từ DB rỗng.
- [ ] Cập nhật `PROGRESS_BE_A.md` bằng bằng chứng và tạo walkthrough sau cùng.

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
