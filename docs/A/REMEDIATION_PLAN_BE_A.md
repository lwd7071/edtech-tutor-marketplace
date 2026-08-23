# Remediation Plan — Backend Thành viên A

## Mục tiêu và ràng buộc

Đưa module của A về đúng boundary trong `PLANBE.md`, bổ sung feedback loop đủ rủi ro và giữ nguyên REST API/schema hiện hữu.

- Không sửa public REST contract nếu chưa có change request riêng.
- Không sửa Flyway migration; mọi nhu cầu schema chuyển cho B.
- Provider sở hữu facade/port, contract DTO và implementation dùng repository nội bộ.
- Consumer chỉ nhận scalar hoặc immutable DTO snapshot, không nhận JPA entity.
- Không thay repository access bằng `JdbcTemplate`, native SQL hay cross-module JPQL.

## Kiến trúc đích

- Query/validation ngắn cần kết quả ngay: public query facade.
- Command thuộc aggregate provider: public command facade; transaction/invariant ở provider.
- Hậu xử lý không cần phản hồi đồng bộ: domain event sau commit.
- Read model tổng hợp: chỉ dùng khi được review như boundary riêng; consumer không tự join bảng provider.

### Contract capability bắt buộc

| Provider | Public capability | Consumer chính |
|---|---|---|
| `auth` | User tồn tại/active; public identity snapshot | common, communication, learning, ranking |
| `teacher` | Resolve teacher theo user; approval/public profile; subject assignment check | catalog, learning, ranking, subject |
| `subject` | Resolve active/public subject snapshot | teacher, catalog, learning |
| `common` attachment | Validate ownership/context; bind attachment; lấy view/URL | communication, learning |
| `booking` (B) | Completed booking/review eligibility; booking statistics | ranking |
| `enrollment` (B) | Package purchase và learning relationship | catalog, learning, communication |
| settings/admin (B) | Bayesian minimum reviews/settings snapshot | ranking |

Contract phân biệt `not found`, `inactive/forbidden` và provider failure bằng error contract hiện có; consumer không tự suy luận `null` khi business rule cần lỗi cụ thể.

## Phase 1 — Inventory và architecture guard

1. Inventory cross-module repository/domain imports, JPQL/native SQL và `JdbcTemplate` table access.
2. Thêm architecture tests (ưu tiên ArchUnit, kết hợp source/query rule): cấm `.repository` ngoài module, entity ngoài public contract và SQL ngoài ownership.
3. Baseline debt phải có owner/task; không dùng ignore tổng quát.
4. Review và loại `FixDbController` khỏi runtime; schema fix chuyển cho B.

**Exit:** guard bắt được fixture vi phạm, mọi finding có task và build không che test bằng skip.

## Phase 2 — Provider facade và contract test

Thứ tự: `auth` → `teacher`/`subject` → attachment → facade từ B.

Với từng provider:

1. Định nghĩa immutable DTO/scalar contract trong public package.
2. Implement facade bằng repository nội bộ.
3. Viết unit test mapping/error và integration/contract test với database.
4. Chỉ migrate consumer sau khi contract xanh.

Không mở repository thành facade, không trả entity trá hình trong DTO và không chuyển transaction ownership sang consumer.

## Phase 3 — Consumer migration

1. `teacher`/`subject`: thay lookup chéo bằng facade; giữ hành vi assign/proposal.
2. `common`: upload dùng identity capability; attachment facade bao trọn validate/bind/view.
3. `learning`: resolve identity/teacher/subject/attachment qua contract; test create/submit/grade/ownership.
4. `communication`: bind/read attachment qua facade; notification dùng identity snapshot hoặc event payload.
5. `ranking`: review eligibility/booking stats/settings qua B; identity/teacher qua snapshot.
6. `catalog`: pricing dùng teacher/subject; purchase/relationship qua B; marketplace dùng read model/facade composition đã review.
7. Xóa import/field/SQL cũ trong cùng tracer bullet sau khi consumer test xanh.

## Test strategy và nghiệm thu

- Unit: state transition, ownership, validation, idempotency, error/DTO mapping.
- Contract: facade success/not-found/inactive/forbidden và transaction semantics.
- Integration: Auth, PricingPackage, Assignment/Submission, Attachment/Chat, Review/Ranking.
- Architecture: repository import, domain entity import và direct SQL boundary.
- Regression: API contract, error code, migration database rỗng và toàn suite.

```powershell
cd backend
mvn test
```

Kết quả bắt buộc: `0 failures`, `0 errors`, `0 skipped` trên Docker/Testcontainers.

Mỗi tracer bullet dùng branch/PR ngắn. Provider contract + test phải tồn tại trước consumer. Cache/event behavior phải giữ tương thích hoặc có regression test. Chỉ chuyển module sang `Done` khi architecture guard xanh và test thực chạy. `walkthrough` chỉ tạo sau khi remediation hoàn tất.
