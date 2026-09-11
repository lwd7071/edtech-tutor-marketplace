# ADR-0005: Finance state machine và an toàn command

- **Trạng thái:** Accepted
- **Bối cảnh:** Refund/Payout có trạng thái lỗi không có command xử lý và các thao tác trên package có thể cạnh tranh với booking.
- **Quyết định:** Refund dùng `PENDING → APPROVED → REFUNDED` hoặc `PENDING → REJECTED`; payout dùng `PENDING → PROCESSING → SUCCEEDED` hoặc từ chối từ `PENDING/PROCESSING`. Domain command kiểm tra state terminal, StudentPackage nhận thời điểm từ service `Clock` và finance action khóa package trước khi kiểm tra/ghi request.
- **Database:** V29 fail-fast với dữ liệu legacy mâu thuẫn, thêm `transferred_at`, partial unique indexes cho request đang active và bảng `finance_command_receipts`.
- **Idempotency:** Finance POST dùng `FinanceCommandExecutor` trong cùng transaction; receipt claim theo `(actor_id, operation, idempotency_key)`, fingerprint SHA-256 có hash account number, replay response khi retry cùng payload và trả conflict khi payload khác. Receipt hết hạn được cleanup theo batch có ShedLock.
- **Hệ quả:** Không tự map/xóa status cũ; phải đối soát proof/ledger/audit trước khi retry migration. V29 đã chạy qua Supabase preflight và apply thành công. Concurrency/rollback/reconciliation integration vẫn là phần nghiệm thu tiếp theo.
