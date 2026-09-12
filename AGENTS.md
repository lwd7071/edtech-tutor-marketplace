# Hướng dẫn làm việc trong EdTech

## Thứ tự đọc bắt buộc

Trước khi bắt đầu mỗi task mới, dù là đọc, chẩn đoán, sửa code, chạy test, commit hay push, phải đọc lại `AGENTS.md` hiện tại.

Sau đó, nếu task có liên quan đến code hoặc tài liệu dự án, đọc theo thứ tự:

1. `CONTEXT.md` — mô hình nghiệp vụ, thuật ngữ và invariant.
2. `docs/STATUS.md` — trạng thái hiện tại, bằng chứng kiểm thử và phần chưa xác minh.
3. Tài liệu architecture hoặc API contract đúng module đang sửa.
4. `docs/adr/` khi thay đổi có thể ảnh hưởng quyết định kiến trúc đã ghi.

Không cần đọc `docs/archive/` trong công việc thông thường. Chỉ mở archive khi điều tra lịch sử hoặc cần tìm lý do của quyết định cũ.

## Trước khi kết thúc một đợt làm việc

- Cập nhật tài liệu contract hoặc architecture nếu hành vi công khai thay đổi.
- Viết lại `docs/STATUS.md` bằng kết quả kiểm thử thật; phân biệt rõ pass, chưa chạy và bị chặn.
- Append một entry ngắn vào `docs/CHANGELOG.md` cho phần đã hoàn thành.
- Tạo hoặc cập nhật ADR nếu khóa một quyết định lâu dài.
- Không ghi secret, giá trị trong `.env.cloud`, JWT hoặc token vào docs/log.
- Không gọi là pass khi mới compile hoặc kiểm tra tĩnh.
- Chạy `scripts/check-docs.ps1` trước khi commit docs.

Tài liệu được commit cùng đợt chức năng để code và context luôn đi cùng nhau.
