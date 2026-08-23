# Báo cáo tiến độ Backend — Thành viên A (Identity & Experience)

> Cập nhật kiểm chứng: 2026-08-23
> Nguồn chân lý: `docs/planning/PLANBE.md`, đặc tả kiến trúc và source code hiện tại.

## Trạng thái tổng quan

A đã tạo code foundation và logic chính cho các module Identity & Experience. Implementation hiện tại **chưa đạt Definition of Done** vì còn dependency xuyên module, SQL vượt ownership, domain entity coupling và thiếu test nghiệp vụ.

Trạng thái tổng thể: **Implemented / Not DoD-compliant / Needs remediation**.

## Trạng thái theo module

| Module | Code foundation | Boundary compliance | Unit test | Integration test nghiệp vụ | Trạng thái |
|---|---|---|---|---|---|
| `common` | Implemented | Fail | Missing | Missing | Needs remediation |
| `auth`, `user` | Implemented | Cần audit đầy đủ | Missing | 1 Auth register case | Not DoD-compliant |
| `teacher`, `subject` | Implemented | Fail | Missing | Missing | Needs remediation |
| `catalog` | Implemented | Fail | Missing | Missing | Needs remediation |
| `communication` | Implemented | Fail | Missing | Missing | Needs remediation |
| `learning` | Implemented | Fail | Missing | Missing | Needs remediation |
| `ranking` | Implemented | Fail | Missing | Missing | Needs remediation |

## Những gì đã có

- `common`: response/error/security/event/storage foundation và attachment flow.
- `auth`, `user`: register/login/refresh, email/password recovery và OAuth foundation.
- `teacher`, `subject`: profile, document, availability, subject và proposal flows.
- `catalog`: pricing package và marketplace endpoints/query foundation.
- `communication`: conversation, message, notification và WebSocket/STOMP foundation.
- `learning`: assignment, submission và grading foundation.
- `ranking`: review, teacher statistics và ranking foundation.

Danh sách này chỉ xác nhận implementation hiện diện, không xác nhận feature đúng đặc tả hoặc đã nghiệm thu.

## DoD dashboard

| Tiêu chí | Trạng thái | Blocker hiện tại |
|---|---|---|
| Endpoint/DTO khớp API contract | Chưa xác minh đầy đủ | Thiếu contract/integration coverage |
| Validation, error code, RBAC, ownership | Chưa xác minh đầy đủ | Thiếu test |
| State transition/transaction boundary được test | Fail | Gần như chưa có test nghiệp vụ |
| Không repository xuyên module | Fail | 12 class, 17 dependency đã xác nhận |
| Không SQL/domain coupling vượt boundary | Fail | Có JdbcTemplate, JPQL/native query và entity reference xuyên module |
| Unit/integration test phù hợp rủi ro | Fail | A có 1 test case nghiệp vụ |
| Migration chạy từ database rỗng | Chưa xác nhận lần này | Testcontainers bị skip vì thiếu Docker |
| Build và toàn bộ test thành công | Fail về nghiệm thu | Maven xanh nhưng 8/8 test bị skip |

## Điều kiện chuyển sang Done

1. Không còn dependency repository/SQL/domain entity ngoài public boundary, trừ ngoại lệ đã review.
2. Facade chỉ trả scalar hoặc immutable DTO snapshot, không trả JPA entity.
3. Có unit, integration và architecture test tương ứng rủi ro.
4. `mvn test` chạy với Docker/Testcontainers: `0 failures`, `0 errors`, `0 skipped`.
5. Endpoint/error/schema behavior được đối chiếu với tài liệu nguồn.

## Tài liệu thực thi

- Bằng chứng: `docs/A/DIAGNOSE_BE_A.md`.
- Thiết kế remediation: `docs/A/REMEDIATION_PLAN_BE_A.md`.
- Checklist nhận việc: `docs/A/TASKS_BE_A.md`.

Không sửa `PLANBE.md`, API contract, ERD hoặc migration trong đợt cập nhật tài liệu này.
