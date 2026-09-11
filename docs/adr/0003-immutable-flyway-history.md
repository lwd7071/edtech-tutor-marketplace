# ADR-0003: Migration Flyway đã áp dụng là bất biến

- **Trạng thái:** Accepted
- **Bối cảnh:** Sửa migration cũ hoặc tự repair checksum có thể che giấu drift schema và làm mất khả năng nâng cấp an toàn.
- **Quyết định:** Migration đã áp dụng không sửa; thay đổi schema phải tạo version mới. Checksum mismatch phải fail rõ ràng.
- **Hệ quả:** Database cũ cần được nâng cấp bằng migration mới hoặc quy trình xử lý có kiểm soát, không reset dữ liệu.
