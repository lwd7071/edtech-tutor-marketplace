# ADR-0002: Ghi notification và realtime sau commit

- **Trạng thái:** Accepted
- **Bối cảnh:** Phát side effect trong transaction có thể tạo notification hoặc message cho dữ liệu đã rollback.
- **Quyết định:** Nghiệp vụ lưu dữ liệu và event trong transaction; listener sau commit ghi notification/conversation, realtime và email adapter xử lý sau đó.
- **Hệ quả:** Có thể retry delivery; transaction chính không giữ lock khi gọi provider hoặc WebSocket.
