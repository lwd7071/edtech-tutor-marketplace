# 8. Optimistic Locking and Data Integrity

Date: 2026-09-12

## Status

Accepted

## Context

Trong quá trình phát triển (Mục 4 - Lỗi dữ liệu/migration), chúng tôi phát hiện một số entity lõi của hệ thống đang đối mặt với nguy cơ xảy ra Race Condition khi bị cập nhật đồng thời (concurrent updates). Nếu hai giao dịch (transaction) cùng lấy một entity và thực hiện thay đổi, giao dịch commit sau có thể ghi đè lên kết quả của giao dịch commit trước mà không hề hay biết (Lost Update).

Các entity bị ảnh hưởng trực tiếp bởi vấn đề này bao gồm: `Invoice`, `TrialRequest`, `Assignment`, `Submission`, `SubjectProposal`, và `TeacherBankAccount`.

Hơn nữa, một số bảng (như `platform_settings`) không được phép sử dụng Optimistic Locking do tính chất singleton (chỉ có duy nhất một dòng) và việc sử dụng Optimistic Locking sẽ phá vỡ logic kiểm tra cấu trúc dữ liệu hiện tại (gây lỗi `duplicate key value violates unique constraint`).

Thêm vào đó, chúng tôi cần sửa đổi các script Flyway. Một số dev có xu hướng "chữa cháy" bằng cách sửa thẳng file migration cũ (ví dụ V16, V19) thay vì tạo file mới, làm phá vỡ tính immutable của Flyway history, gây lỗi khi deploy.

## Decision

Để giải quyết các vấn đề trên, chúng tôi đưa ra quyết định kiến trúc như sau:

1. **Áp dụng Optimistic Locking:** Thêm annotation `@Version` vào 6 entity lõi có rủi ro cao (`Invoice`, `TrialRequest`, `Assignment`, `Submission`, `SubjectProposal`, `TeacherBankAccount`). Cơ chế này dựa vào version field để đảm bảo giao dịch cập nhật dữ liệu phải thao tác trên bản ghi mới nhất. Nếu version bị lệch, Hibernate sẽ ném ra `OptimisticLockException` để hủy giao dịch và ngăn chặn ghi đè dữ liệu.
2. **Cập nhật `@SQLDelete` Soft-delete:** Bổ sung điều kiện version `AND version = ?` vào annotation `@SQLDelete` của các entity có dùng optimistic locking để đảm bảo tính nhất quán giữa lệnh soft delete và version check.
3. **Tuyệt đối Append-only Migration:** Nghiêm cấm sửa chữa, thêm bớt constraint vào các file migration Flyway đã được commit trước đó (`V1` đến `V29`). Bất kỳ sự thay đổi schema hoặc dữ liệu nào cũng phải được thực hiện thông qua file migration mới nhất, trong trường hợp này là `V30__fix_data_constraints_and_locking.sql`.
4. **Không áp dụng `@Version` cho Singleton Entity:** Đối với các entity như `PlatformSettings` mang tính chất singleton, không sử dụng optimistic locking để bảo vệ logic constraint độc bản của database.

## Consequences

- **Positive:** Dữ liệu tài chính, thanh toán, yêu cầu và bài tập sẽ được bảo vệ tuyệt đối khỏi lỗi Lost Update do concurrent requests.
- **Positive:** Cơ sở dữ liệu sẽ giữ được tính toàn vẹn (integrity) cao nhất mà không bị giảm hiệu suất (do Optimistic Locking tiêu tốn rất ít tài nguyên so với Pessimistic Locking).
- **Positive:** Flyway history luôn sạch sẽ, đảm bảo tính repeatable và deploy an toàn lên mọi môi trường.
- **Negative:** Đội ngũ frontend và backend (Service Layer) phải chuẩn bị xử lý ngoại lệ `OptimisticLockException` (hoặc HTTP 409 Conflict) và hướng dẫn người dùng tải lại trang/thử lại hành động khi xảy ra tranh chấp dữ liệu.
